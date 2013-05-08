package com.au_craft.GGTeleport;


import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class GGTeleport extends JavaPlugin implements Listener {
	
	private int[] blockBlackListArray;
	private double[] testLocation={0,0,0,0};
	private int xRadius;
	private int zRadius;
	private int maxTries;
	private String[] jarVersion = {"0.4","0.5"};
	
	@Override
	public void onEnable(){
		
		loadConfiguration();
			
		getLogger().info("Enabled");
		getServer().getPluginManager().registerEvents(this, this);
	}
		
	@Override
	public void onDisable(){
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if (!(sender instanceof Player)){ //Check for Player as Sender
			sender.sendMessage("[GG Teleport] You need to be a player in game to access this command!");
			return true;
		} 
			
		Player player = (Player) sender;
				
		if (cmd.getName().equalsIgnoreCase("tpr")){
			if (args.length == 0) {
				if (player.hasPermission("GGT.use.default")){
					sender.sendMessage("[GG Teleport] Started with a " + xRadius + " x " + zRadius + " radius!");
					tpr(player, xRadius, zRadius);
				} else {
					sender.sendMessage("[GG Teleport] You do not have high enough permission");
				}
			} else if (args.length == 1){
				switch (args[0]) {
					case "reload": 
						if (player.hasPermission("GGT.reload")){
							reloadConfiguration();
							sender.sendMessage("[GG Teleport] Config Reloaded!");
						} else {
							sender.sendMessage("[GG Teleport] You do not have high enough permission");
						}
						break;
					default:
						if (player.hasPermission("GGT.use.custom")){
							if (testIfNumber(args[0]) == false){
								sender.sendMessage("[GG Teleport] '" + args[0] + "' is not a valid number.");
							} else {
								sender.sendMessage("[GG Teleport] Started with a " + args[0] + " radius!");
								tpr(player, Integer.parseInt(args[0]), Integer.parseInt(args[0]));
							}
						} else {
							sender.sendMessage("[GG Teleport] You do not have high enough permission");
						}
						break;
				}
			} else if (args.length == 2){
				if (player.hasPermission("GGT.custom")){
					if (testIfNumber(args[0]) == false || testIfNumber(args[1]) == false){
						sender.sendMessage("[GG Teleport] '" + args[0] + "' or '" + args[1] + "' is not a valid number.");
					} else {
						sender.sendMessage("[GG Teleport] Started with a " + args[0] + " x " + args[1] + " radius!");
						tpr(player, Integer.parseInt(args[0]), Integer.parseInt(args[1]));
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
		checkConfigCompatibility();
	}

	public void reloadConfiguration(){
		this.reloadConfig();
		getBlocksBlackList();
		getRadius();
		getMaxTryCount();
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
	
	private void tpr(Player player, int xRadius, int zRadius) {
		Location currentLocation = player.getLocation();
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
			
			player.teleport(finalLocation);
			player.sendMessage("[GG Teleport] Complete!");
		} else {
			player.sendMessage("[GG Teleport] Failed!");
		}
		testLocation[0] = 0;
	}
}
