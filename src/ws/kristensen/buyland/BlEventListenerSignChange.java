package ws.kristensen.buyland;

import org.bukkit.Location;
import org.bukkit.World;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import ws.kristensen.buyland.BuyLand;


import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class BlEventListenerSignChange extends JavaPlugin implements Listener  {
	public static BuyLand plugin;

	public BlEventListenerSignChange(BuyLand instance) {
		plugin = instance;
	}
	
    public boolean isSignWithinRegion(Location signLocation, Location protectedRegionMinimum, Location protectedRegionMaximum) {
        if (protectedRegionMinimum.getX() < signLocation.getX() && signLocation.getX() < protectedRegionMaximum.getX()) {
            if (protectedRegionMinimum.getY() < signLocation.getY() && signLocation.getY() < protectedRegionMaximum.getY()) {
                if (protectedRegionMinimum.getZ() < signLocation.getZ() && signLocation.getZ() < protectedRegionMaximum.getZ()) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
	public void onSignChange(SignChangeEvent event) {
       if (event.getLine(0).contains("[BuyLand]") || event.getLine(0).equalsIgnoreCase("[BuyLand]")) {
           //Get the player making the change
           Player player = event.getPlayer();
           //get the world involved
           World world = player.getWorld();

           //get the sign location
           Location signLocation = event.getBlock().getLocation();

           //See if the player has rights to make a sign
           if (player.hasPermission("buyland.signcreate") || player.hasPermission("buyland.all")) {
               //Make sure the first line on the sign is "[Buyland]" with the correct case
               event.setLine(0, "[BuyLand]");

               //Get the region manager for the world
               RegionManager regionManager = plugin.getWorldGuard().getRegionManager(world);

               //Get the region name on the sign
               String regionName = event.getLine(2).toLowerCase();

               //Try to get the protected region indicated
               ProtectedRegion protectedRegion = regionManager.getRegionExact(regionName);
               
               //Make sure the region exists
               if (protectedRegion == null) {
                   //Region does not exist.
                   event.setLine(0, "ERROR!");
                   event.setLine(1, "ERROR!");
                   event.setLine(2, "Invalid Region");
               } else {
                   //Get protected region min and max locations
                   Location protectedRegionMinimum = new Location(world, 
                                                                  protectedRegion.getMinimumPoint().getBlockX(), 
                                                                  protectedRegion.getMinimumPoint().getBlockY(), 
                                                                  protectedRegion.getMinimumPoint().getBlockZ()
                                                                 );
                   Location protectedRegionMaximum = new Location(world, 
                                                                  protectedRegion.getMaximumPoint().getBlockX(), 
                                                                  protectedRegion.getMaximumPoint().getBlockY(), 
                                                                  protectedRegion.getMaximumPoint().getBlockZ()
                                                                 );
                   //See if the sign is within the region
                   if (isSignWithinRegion(signLocation, protectedRegionMinimum, protectedRegionMaximum)) { 
                       //Deny placing a sign within its own region!
                       event.setLine(0, "ERROR!");
                       event.setLine(1, "ERROR!");
                       event.setLine(2, "Place sign");
                       event.setLine(3, "outside region");
                       plugin.sendMessageWarning(player, "Placing a sign inside its own region will cause errors!");
                       plugin.sendMessageWarning(player, "Please place it outside of the region!");
                   } else {
                       boolean isSignCreationAllowed = false;
                       //See if the sign is for sale
                       if (event.getLine(1).equalsIgnoreCase("For Sale") || event.getLine(1).equalsIgnoreCase("ForSale")) {
                           //make sure the case is correct on the sign
                           event.setLine(1, "For Sale");
                           //make sure region is buyable
                           if (protectedRegion.getFlag(DefaultFlag.BUYABLE) == true) {
                               //Get the price of the region
                               Double regionPrice = plugin.getRegionPurchasePrice(protectedRegion);
                               
                               //set line 3 of the sign to the price
                               event.setLine(3, regionPrice.toString());
                               
                               //create a sign config for the sign being created
                               //plugin.getSignConfig().set("sign." + regionName, plugin.locationToString(signLocation));

                               //set create success flag
                               isSignCreationAllowed = true;                               
                           } else {
                               event.setLine(0, "ERROR!");
                               event.setLine(1, "ERROR!");
                               event.setLine(2, "Not for sale");
                               plugin.sendMessageWarning(player, "Region must be for sale when placing a for sale sign.");
                           }
                       } else if (event.getLine(1).equalsIgnoreCase("For Rent") || event.getLine(1).equalsIgnoreCase("ForRent")) {
                           //Make sure the case is correct on the sign
                           event.setLine(1, "For Rent");

                           //make sure region is rentable
                           if (plugin.getRentConfig().contains("rent." + regionName + ".time") == true) {
                               //Make sure that line 3 is in the format of:   [TimeQuantity] [Sec/Min/Hr/Day/Wk]
                               //    That way the parameter can be passed on as the 2nd and 3rd parameter to:  /rentland [regionName] [TimeQuantity] [Sec/Min/Hr/Day/Wk]
                               String parts[] = event.getLine(3).split(" ");
                               boolean isValidTimeQuantity = false;
                               boolean isValidTimeFlag = false;

                               if (parts.length == 2) {
                                   //Check the TimeQuantity portion
                                   try {
                                       Integer.parseInt(parts[0]); 
                                       isValidTimeQuantity = true;
                                   } catch(NumberFormatException e) {
                                   }
                                   
                                   //Check the [Sec/Min/Hr/Day/Wk] portion
                                   if (parts[1].equalsIgnoreCase("S") || parts[1].equalsIgnoreCase("Sec") || parts[1].equalsIgnoreCase("Second")) { parts[1] = "Second"; isValidTimeFlag = true;}
                                   if (parts[1].equalsIgnoreCase("M") || parts[1].equalsIgnoreCase("Min") || parts[1].equalsIgnoreCase("Minute")) { parts[1] = "Minute"; isValidTimeFlag = true;}
                                   if (parts[1].equalsIgnoreCase("H") || parts[1].equalsIgnoreCase("Hr")  || parts[1].equalsIgnoreCase("Hour")  ) { parts[1] = "Hour";   isValidTimeFlag = true;}
                                   if (parts[1].equalsIgnoreCase("D") ||                                     parts[1].equalsIgnoreCase("Day")   ) { parts[1] = "Day";    isValidTimeFlag = true;}
                                   if (parts[1].equalsIgnoreCase("W") || parts[1].equalsIgnoreCase("Wk")  || parts[1].equalsIgnoreCase("Week")  ) { parts[1] = "Week";   isValidTimeFlag = true;}
                               }
                               
                               //see if they are valid settings
                               if (isValidTimeQuantity && isValidTimeFlag) {
                                   //update line 3 with the properly cased [Sec/Min/Hr/Day/Wk]
                                   event.setLine(3, parts[0] + " " + parts[1]);
                                   
                                   //create a sign config for the sign being created
                                  // plugin.getSignConfig().set("sign." + regionName, plugin.locationToString(signLocation));
                                   
                                   //set create success flag
                                   isSignCreationAllowed = true;                                                                  
                               } else {
                                   event.setLine(0, "ERROR!");
                                   event.setLine(1, "ERROR!");
                                   plugin.sendMessageWarning(player, "Line 4 must be in format of [TimeQuantity] [Sec/Min/Hr/Day/Wk].");
                                   plugin.sendMessageInfo(player, "Example: 1 Hour");
                               }
                           } else {
                               event.setLine(0, "ERROR!");
                               event.setLine(1, "ERROR!");
                               event.setLine(2, "Not for rent");
                               plugin.sendMessageWarning(player, "Region must be for rent when placing a for rent sign.");
                           }
                       } else { 	
                           event.setLine(0, "ERROR!");
                           event.setLine(1, "ERROR!");
                           plugin.sendMessageWarning(player, "Invalid Command. Must be: 'For Rent' or 'For Sale'.");
                           
                       }
    
                       //See if the sign creation was allowed
                       if (isSignCreationAllowed) {
                           if (plugin.registerBuyLandSign(world, regionName, signLocation, event.getLines())) {
                               //Notify the player
                               plugin.sendMessageInfo(player, "BuyLand Sign Created!");
                           } else {
                               event.setLine(0, "ERROR!");
                               event.setLine(1, "ERROR!");                               
                               plugin.sendMessageWarning(player, "Validation Error in recording Buyland sign.");
                           }
                       }
                   }

               }
           } else {
               //Change the sign to indicate the player has no permission to make a Buyland sign
               plugin.sendMessageWarning(player, "You Do Not Have Permission To Create A BuyLand Sign!");
               event.setLine(0, "ERROR!");
               event.setLine(1, "No permission");
               event.setLine(2, "to create a");
               event.setLine(3, "buyland sign!");
           }
       }
	}
}
