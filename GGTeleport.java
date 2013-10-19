package com.au_craft.GGTeleport;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.gravitydevelopment.updater.Updater;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class GGTeleport extends JavaPlugin implements Listener {
	
	private int[] blockBlackListArray;
	private double[] testLocation={0,0,0,0};
	private int xRadius;
	private int zRadius;
	private int maxTries;
	private String[] jarVersion = {"0.8", "0.9", "0.9.1", "0.9.2"};
	private String errorReason = "";
	private double[] portalPos1={0,0,0};
	private double[] portalPos2={0,0,0};
	private String portalWorld;
	private double[] playerPos={0,0,0};
	private boolean checked = false;
	BukkitTask timing;
	
	public static boolean update = false;
	public static String name = "";
	public static String version = "";
	
	@Override
	public void onEnable(){
		
		loadConfiguration();
		if (getConfig().getBoolean("checkForUpdates")){
			Updater updater = new Updater(this, 57057, this.getFile(), Updater.UpdateType.DEFAULT, false);
			update = updater.getResult() == Updater.UpdateResult.UPDATE_AVAILABLE;
			if (update){
				name = updater.getLatestName();
				version = updater.getLatestGameVersion();
				getLogger().info("Restart Bukkit to update to " + name + " for " + version);
			}
		} else {
			getLogger().warning("Update Checking is Disabled. It is advised to enable it in plugins/GGTeleport/config.yml");
		}
		
		getLogger().info("Enabled");
		getServer().getPluginManager().registerEvents(this, this);
	}
		
	@Override
	public void onDisable(){
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if (sender instanceof BlockCommandSender){
			BlockCommandSender blockSender = (BlockCommandSender)sender;
			if (cmd.getName().equalsIgnoreCase("tpr")){
				if (args.length < 1){
					getLogger().info("Not enough arguments from CommandBlock at (" + blockSender.getBlock().getX() + "," + blockSender.getBlock().getY() + "," + blockSender.getBlock().getZ() + ")");
				} else {
					if (args.length == 1) {
						tpr(Bukkit.getPlayer(args[0]), blockSender.getBlock().getLocation(), sender, xRadius, zRadius);
					} else {
						String coords[] = args[1].split(",");
						tpr(Bukkit.getPlayer(args[0]), blockSender.getBlock().getLocation(), sender, Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
					}
					
				}
			}
			
			return true;
		} else if (!(sender instanceof Player)){ //Check for Player as Sender
			sender.sendMessage("[GG Teleport] You need to be a player in game to access this command!");
			return true;
		}
		
		ArrayList<String> argsList = new ArrayList<String>();
		for (String arg:args){
			argsList.add(arg);
		}
					
		Player target = (Player) sender;
		
		for (int i = 0; i<argsList.size(); i++){
			String string = argsList.get(i);
			if (string.toLowerCase().startsWith("p:")){
				if (sender.hasPermission("GGT.use.others")){
					try {
						target = getServer().getPlayer(string.substring(2));
					} catch (NullPointerException e){
						sender.sendMessage(string.substring(2)+" is not online.");
						return true;
					}
					argsList.remove(i);
				}else {
					sender.sendMessage("You do not have sufficient Permission");
					return true;
				}
			}
		}
		Player center = (Player) sender;
		for (int i = 0; i<argsList.size(); i++){
			String string = argsList.get(i);
			if (string.toLowerCase().startsWith("c:")){
				if (sender.hasPermission("GGT.use.center")){
					try {
						if (string.equals("c:")){
							center = target;
						}else{
							center = getServer().getPlayer(string.substring(2));
						}
					} catch (NullPointerException e){
						sender.sendMessage(string.substring(2)+" is not online.");
						return true;
					}
					argsList.remove(i);
				}else {
					sender.sendMessage("You do not have sufficient Permission");
					return true;
				}
			}
		}
		
		if (cmd.getName().equalsIgnoreCase("tpr")){
			if (argsList.size() == 0){
				if (sender.hasPermission("GGT.use.default")){
					sender.sendMessage("[GG Teleport] Teleporting "+ target.getName() +" in a " + xRadius + " x " + zRadius + " radius around " + center.getName());
					tpr(target, center.getLocation(), (Player) sender, xRadius, zRadius);
				} else {
					sender.sendMessage("[GG Teleport] You do not have high enough permission");
				}
			} else if (argsList.size() == 1){
				switch (argsList.get(0)) {
					case "reload": 
						if (sender.hasPermission("GGT.reload")){
							reloadConfiguration();
							sender.sendMessage("[GG Teleport] Config Reloaded!");
						} else {
							sender.sendMessage("[GG Teleport] You do not have high enough permission");
						}
						break;
					default:
						if (sender.hasPermission("GGT.use.custom")){
							if (testIfNumber(argsList.get(0)) == false){
								sender.sendMessage("[GG Teleport] '" + argsList.get(0) + "' is not a valid number.");
							} else {
								sender.sendMessage("[GG Teleport] Teleporting " + target.getName() + " in a " + argsList.get(0) + " radius around " + center.getName());
								tpr(target, center.getLocation(), (Player) sender, Integer.parseInt(argsList.get(0)), Integer.parseInt(argsList.get(0)));
							}
						} else {
							sender.sendMessage("[GG Teleport] You do not have high enough permission");
						}
						break;
				}
			} else if (argsList.size() == 2){
				if (sender.hasPermission("GGT.custom")){
					if (testIfNumber(argsList.get(0)) == false || testIfNumber(argsList.get(1)) == false){
						sender.sendMessage("[GG Teleport] '" + argsList.get(0) + "' or '" + argsList.get(1) + "' is not a valid number.");
					} else {
						sender.sendMessage("[GG Teleport] Teleporting " + target.getName() + " in a " + argsList.get(0) + " x " + argsList.get(1) + " radius around " + center.getName());
						tpr(target, center.getLocation(), (Player) sender, Integer.parseInt(argsList.get(0)), Integer.parseInt(argsList.get(1)));
					}
				} else {
				sender.sendMessage("[GG Teleport] You do not have high enough permission");				
				}
			} else {
				sender.sendMessage("[GG Teleport] Too many Arguments");
			}
		}
		return true;
	}

	private boolean testIfNumber(String s){
		try {
			int n = Integer.parseInt(s);
			if (n < 1){
				return false;
			}
		} catch (NumberFormatException nfe){
			return false;
		}
		return true;
	}
	
	public void loadConfiguration(){
		getConfig().options().copyDefaults(true);
		saveDefaultConfig();
		getBlocksBlackList();
		getMaxTryCount();
		getRadius();
		getPortal();
		checkConfigCompatibility();
	}

	public void reloadConfiguration(){
		this.reloadConfig();
		getBlocksBlackList();
		getRadius();
		getMaxTryCount();
		getPortal();
		checkConfigCompatibility();
		this.getLogger().info("Config Reloaded");
	}
	
	public void getBlocksBlackList(){
		List<Integer> blockBlackList = getConfig().getIntegerList("blockBlackList");
		blockBlackListArray = new int[blockBlackList.size()];
		for (int i = 0; i<blockBlackList.size(); i++){
			int x = blockBlackList.get(i);
			blockBlackListArray[i] = x;
		}
	}
	
	public void getMaxTryCount(){
		maxTries = getConfig().getInt("maximumTries");
		if (maxTries == -1){
			maxTries = 2147483647;
		} else if (maxTries < -1){
			maxTries = 0;
		}
	}
	
	public void getRadius(){
		xRadius = getConfig().getInt("xRadius");
		zRadius = getConfig().getInt("zRadius");
	}
	
	public void getPortal(){
		portalWorld = getConfig().getString("portal.world");
		portalPos1[0] = getConfig().getDouble("portal.pos1.X");
		portalPos1[1] = getConfig().getDouble("portal.pos1.Y");
		portalPos1[2] = getConfig().getDouble("portal.pos1.Z");
		portalPos2[0] = getConfig().getDouble("portal.pos2.X");
		portalPos2[1] = getConfig().getDouble("portal.pos2.Y");
		portalPos2[2] = getConfig().getDouble("portal.pos2.Z");
	}

	public void checkConfigCompatibility(){
		String configVersion = getConfig().getString("Version");
		boolean configIsCompatible = false;
		for (String compatibleVersion: jarVersion){
			if (configVersion.equals(compatibleVersion)){
				configIsCompatible = true;
			}
		}
		if (!configIsCompatible){
			getLogger().warning("Config is incompatible. Please delete config and Reload!");
			getLogger().warning("Using Default values!");
		}
		
	}
	
	private void loadChunk(World w, double x, double z){
		Chunk chunk = w.getChunkAt((int) x, (int) z);
		chunk.load(true);
		getLogger().info("Loading Chunk");		
	}
	
	private void tpr(Player target, Location center, CommandSender sender, int xRadius, int zRadius) {
		Location currentLocation = center;
		World w = currentLocation.getWorld();
		Random random = new Random();
		
		int tooManyTimes = 0;
		
		while (testLocation[0] != 1 && tooManyTimes < maxTries){
									
			double x = random.nextInt(xRadius * 2) - xRadius;
			double z = random.nextInt(zRadius * 2) - zRadius;
			
			testLocation[1] = x + currentLocation.getX();
			testLocation[3] = z + currentLocation.getZ();
			testLocation[2] = w.getHighestBlockYAt((int) testLocation[1], (int) testLocation[3]) - 1;
			
			int testBlock = w.getBlockTypeIdAt((int) testLocation[1], (int) testLocation[2], (int) testLocation[3]);
			
			int isSafe = 0;
			for (int i=0; i < blockBlackListArray.length; i++){
				if (testBlock != blockBlackListArray[i]){
					isSafe++;
				}
			}
			if (isSafe == blockBlackListArray.length){
				testLocation[0] = 1;
			} else {
				testLocation[0] = 0;
			}

		tooManyTimes++;
		}
		if (tooManyTimes >= maxTries){
			errorReason = "Couldn't find a safe teleportation location";
			
		}
		
		if (testLocation[0] == 1){
			Location finalLocation = currentLocation;
			
			if (testLocation[1] < 0){
				finalLocation.setX(testLocation[1]+1);
			} else {
				finalLocation.setX(testLocation[1]);
			}
			
			finalLocation.setY(testLocation[2] + 1);
			
			if (testLocation[3] < 0){
				finalLocation.setZ(testLocation[3]+1);
			} else {
				finalLocation.setZ(testLocation[3]);
			}
			loadChunk(w, finalLocation.getX(), finalLocation.getZ());
			target.teleport(finalLocation);
			checkSafe(target, finalLocation);
		} else {
			sender.sendMessage("[GG Teleport] Failed: " + errorReason);
		}
		testLocation[0] = 0;
	}
	
	public void checkSafe(final Player targetCheck, final Location finalLocationCheck){
		if (checked){
			checked = false;
			if (targetCheck.getLocation().getY() < finalLocationCheck.getY() - 1){
				getLogger().info("Saving Player!");
				Location currentLocation = targetCheck.getLocation();
				currentLocation.setY(finalLocationCheck.getY());
				targetCheck.teleport(currentLocation);
			}
		} else {
			checked = true;
			timing = new BukkitRunnable(){
				public void run(){
					checkSafe(targetCheck, finalLocationCheck);
					getLogger().info("Check done");
				}
			}.runTaskLater(this, 5);
		}
	}
	@EventHandler
	private void atPortal(PlayerPortalEvent event){
		Player player = event.getPlayer();
		playerPos[0] = player.getLocation().getX();
		playerPos[1] = player.getLocation().getY();
		playerPos[2] = player.getLocation().getZ();
		if (player.getLocation().getWorld().getName().toString().toLowerCase().equals(portalWorld)){
			if ((portalPos1[0] <= playerPos[0] && playerPos[0] <= portalPos2[0]) || (portalPos2[0] <= playerPos[0] && playerPos[0] <= portalPos1[0])){
				if ((portalPos1[1] <= playerPos[1] && playerPos[1] <= portalPos2[1]) || (portalPos2[1] <= playerPos[1] && playerPos[1] <= portalPos1[1])){
					if ((portalPos1[2] <= playerPos[2] && playerPos[2] <= portalPos2[2]) || (portalPos2[2] <= playerPos[2] && playerPos[2] <= portalPos1[2])){
						event.setCancelled(true);
						tpr(player, player.getLocation(), player, xRadius, zRadius);
					}
				}
			}
		}
	}
}
