package me.datdenkikniet.ItemDinges;

import me.datdenkikniet.ItemDinges.config.Config;
import me.datdenkikniet.ItemDinges.config.CustomConfig;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ItemDingesMain extends JavaPlugin implements Listener
{
	private Config itemConfig;
	private CustomConfig cfg;
	private Config list;
	private ArrayList<InventoryItem> items = new ArrayList<>();
	private ArrayList<String> worlds = new ArrayList<>();
	private boolean usingIds;

	public void onEnable()
	{

		// IDK init configs.
		this.cfg = new CustomConfig( this );
		this.itemConfig = new Config( "items", this.cfg );
		this.list = new Config( "list", this.cfg );

		// Save the itemConfig if it doesn't already exist.
		if ( this.itemConfig.file == null || this.itemConfig.fileConfig == null ) {
			this.cfg.saveDefaultConfig( this.itemConfig );
		}

		// Save the list config if it doesn't already exist.
		if ( this.list.file == null || this.list.fileConfig == null ) { this.cfg.saveDefaultConfig( this.list ); }

		// init usingIds.
		this.usingIds = this.cfg.getCustomConfig( this.itemConfig ).getBoolean( "using_ids" );

		// Save the new list.
		this.cfg.saveCustomConfig( this.list );

		// Register this to the event handler. EW.
		getServer().getPluginManager().registerEvents( this, this );

		// Load and init the items.
		loadItems();

		// Load and init the worlds.
		loadWorlds();

	}

	// Adds an item to the list of items.
	public void addItem( InventoryItem it )
	{
		this.items.add( it );
	}

	/**
	 * Holy shit. This loads the items..
	 */
	public void loadItems()
	{

		// Keep this to a var so we can use it over and over..
		ConfigurationSection itemSection = this.cfg.getCustomConfig( this.itemConfig ).getConfigurationSection( "items" );

		if ( !this.usingIds ) {

			// Iterate through the keys.
			for ( String key : itemSection.getKeys( false ) ) {

				// Load the name and amount of the items.
				int amount = itemSection.getInt( key + ".amount" );
				String name = ChatColor.translateAlternateColorCodes( '&', itemSection.getString( key + ".name" ) );

				// The position in the inventory.
				int pos = Integer.valueOf( key );

				// Load the lists for command and lore.
				List<String> lore = itemSection.getStringList( key + ".lore" );
				List<String> commands = itemSection.getStringList( key + ".commands" );

				// Type? Maybe data value or id.
				String type = itemSection.getString( key + ".type" );

				// Creates and loads the itemstack.
				ItemStack st = toItem( type, amount );

				// Get the damage value and type of item.
				short damage = st.getDurability();
				Material mat = st.getType();

				// The InventoryItem that adds itself using addItem( InventoryItem )
				new InventoryItem( this, pos, mat, amount, lore, name, commands, damage );

			}

		}
		else {

			// Iterate through the keys.
			for ( String key : itemSection.getKeys( false ) ) {

				try {

					// Load the name and amount of the items.
					int amount = itemSection.getInt( key + ".amount" );
					String name = ChatColor.translateAlternateColorCodes( '&', itemSection.getString( key + ".name" ) );

					// Keeps the position in the inventory.
					Integer pos = Integer.valueOf( key );

					// Create the blank material and damage vars.
					Material mat;
					short damage;

					// Load the lists for lore and commands.
					List<String> lore = itemSection.getStringList( key + ".lore" );
					List<String> commands = itemSection.getStringList( key + ".commands" );

					// Split the type via a colon then load it to an array.
					String[] type = itemSection.getString( key + ".type" ).split( ":" );

					if ( type.length == 1 ) {

						// Try and load the inventory without a damage value.
						try {
							mat = Material.valueOf( this.cfg.getCustomConfig( this.list ).getString( type[ 0 ] ) );
							damage = 0;
						} catch ( Exception ex ) {
							ex.printStackTrace();
							getLogger().severe( "Something went wrong while loading the items! It went wrong for the item with the name: " + key + ". Disabling plugin..." );
							getServer().getPluginManager().disablePlugin( this );
							return;
						}

					}
					else {

						// Try and load the inventory using a damage value.
						try {
							mat = Material.valueOf( this.cfg.getCustomConfig( this.list ).getString( type[ 0 ] ) );
							damage = Short.valueOf( type[ 1 ] );
						} catch ( Exception ex ) {
							ex.printStackTrace();
							getLogger().severe( "Something went wrong while loading the items! It went wrong for the item with the name: " + key + ". Disabling plugin..." );
							getServer().getPluginManager().disablePlugin( this );
							return;
						}

					}

					new InventoryItem( this, pos, mat, amount, lore, name, commands, damage );

				} catch ( Exception ex ) {

					ex.printStackTrace();
					getLogger().severe( "Something went wrong while loading the items! It went wrong for the item with the name: " + key + ". Disabling plugin..." );
					getServer().getPluginManager().disablePlugin( this );
					return;

				}

			}

		}

	}

	/**
	 * Pretty simple.. Ads a list of strings to a list? O.o
	 */
	public void loadWorlds()
	{
		this.worlds.addAll( this.cfg.getCustomConfig( this.itemConfig ).getStringList( "worlds" ) );
	}

	@EventHandler
	public void join( PlayerJoinEvent e )
	{
		if ( worlds.contains( e.getPlayer().getLocation().getWorld().getName() ) && ( !e.getPlayer().hasPermission( "hubinventory.clear" ) ) ) {

			e.getPlayer().getInventory().clear();
			for ( InventoryItem i : this.items )
				e.getPlayer().getInventory().setItem( i.getPosition(), i.getStack() );

			e.getPlayer().updateInventory();

		}
	}

	@EventHandler
	public void click( InventoryClickEvent e )
	{

		if ( worlds.contains( e.getWhoClicked().getLocation().getWorld().getName() ) && ( !e.getWhoClicked().hasPermission( "hubinventory.move" ) ) ) {
			e.setCancelled( true );
			( (Player) e.getWhoClicked() ).updateInventory();
		}
	}

	@EventHandler
	public void click( InventoryDragEvent e )
	{
		if ( worlds.contains( e.getWhoClicked().getLocation().getWorld().getName() ) && !e.getWhoClicked().hasPermission( "hubinventory.move" ) ) {
			e.setCancelled( true );
			( (Player) e.getWhoClicked() ).updateInventory();
		}
	}

	@EventHandler
	public void switchWorld( PlayerChangedWorldEvent e )
	{

		// TODO add some saving?

		if ( worlds.contains( e.getPlayer().getLocation().getWorld().getName() ) && !e.getPlayer().hasPermission( "hubinventory.clear" ) ) {
			e.getPlayer().getInventory().clear();
			for ( InventoryItem i : this.items )
				e.getPlayer().getInventory().setItem( i.getPosition(), i.getStack() );
		}
	}

	@EventHandler
	public void interact( PlayerInteractEvent e )
	{
		if (
				( e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK ) &&
				e.getPlayer().hasPermission( "hubinventory.commands" ) &&
				worlds.contains( e.getPlayer().getWorld().getName() ) &&
				e.getPlayer().getItemInHand() != null &&
				e.getPlayer().getItemInHand().getItemMeta().getDisplayName() != null
		) {
			for ( InventoryItem item : items ) {
				if ( item.getName().equals( e.getPlayer().getItemInHand().getItemMeta().getDisplayName() ) ) {
					if ( item.getCommands() != null && !item.getCommands().isEmpty() ) {
						for ( String cmd : item.getCommands() ) {
							e.getPlayer().performCommand( cmd );
						}
					}
					e.setCancelled( true );
					return;
				}
			}
		}
	}

	@EventHandler
	public void drop( PlayerDropItemEvent e )
	{
		if ( worlds.contains( e.getPlayer().getLocation().getWorld().getName() ) && !e.getPlayer().hasPermission( "hubinventory.clear" ) ) {
			e.setCancelled( true );
			e.getPlayer().updateInventory();
		}
	}

	/**
	 * I'm not fucking touching this.
	 *
	 * @param string The name of the item?
	 * @param amount the number of items to be in the stack.
	 * @return an itemstack made from the string and amount.
	 */
	public ItemStack toItem( String string, int amount )
	{
		try {
			if ( string.equalsIgnoreCase( "carrot" ) ) {
				Material mat = Material.CARROT_ITEM;
				return new ItemStack( mat, amount );
			}
			if ( string.equalsIgnoreCase( "nether wart" ) ) {
				Material mat = Material.NETHER_STALK;
				return new ItemStack( mat, amount );
			}
			Material mat = Material.valueOf( string.toUpperCase().replace( " ", "_" ) );
			return new ItemStack( mat, amount );
		} catch ( Exception ex ) {
			String[] list = string.split( " " );
			if ( list.length == 1 ) {
				if ( list[ 0 ].equalsIgnoreCase( "andesite" ) ) {
					return new ItemStack( Material.STONE, amount, (short) 5 );
				}
				if ( list[ 0 ].equalsIgnoreCase( "granite" ) ) {
					return new ItemStack( Material.STONE, amount, (short) 1 );
				}
				if ( list[ 0 ].equalsIgnoreCase( "diorite" ) ) {
					return new ItemStack( Material.STONE, amount, (short) 3 );
				}
				if ( list[ 0 ].equalsIgnoreCase( "podzol" ) ) {
					return new ItemStack( Material.DIRT, amount, (short) 2 );
				}
				return null;
			}
			if ( list.length == 2 ) {
				if ( list[ 1 ].equalsIgnoreCase( "wool" ) ) {
					if ( list[ 0 ].equalsIgnoreCase( "orange" ) ) {
						return new ItemStack( Material.WOOL, amount, (short) 1 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "magenta" ) ) {
						return new ItemStack( Material.WOOL, amount, (short) 2 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "yellow" ) ) {
						return new ItemStack( Material.WOOL, amount, (short) 4 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "lime" ) ) {
						return new ItemStack( Material.WOOL, amount, (short) 5 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "pink" ) ) {
						return new ItemStack( Material.WOOL, amount, (short) 6 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "gray" ) ) {
						return new ItemStack( Material.WOOL, amount, (short) 7 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "cyan" ) ) {
						return new ItemStack( Material.WOOL, amount, (short) 9 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "purple" ) ) {
						return new ItemStack( Material.WOOL, amount, (short) 10 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "blue" ) ) {
						return new ItemStack( Material.WOOL, amount, (short) 11 );
					}
					if ( list[ 1 ].equalsIgnoreCase( "brown" ) ) {
						return new ItemStack( Material.WOOL, amount, (short) 12 );
					}
					if ( list[ 1 ].equalsIgnoreCase( "green" ) ) {
						return new ItemStack( Material.WOOL, amount, (short) 13 );
					}
					if ( list[ 1 ].equalsIgnoreCase( "red" ) ) {
						return new ItemStack( Material.WOOL, amount, (short) 14 );
					}
					if ( list[ 1 ].equalsIgnoreCase( "black" ) ) {
						return new ItemStack( Material.WOOL, amount, (short) 15 );
					}
					return null;
				}
				if ( list[ 1 ].equalsIgnoreCase( "dye" ) ) {
					if ( list[ 0 ].equalsIgnoreCase( "orange" ) ) {
						return new ItemStack( Material.INK_SACK, amount, (byte) ( 15 - DyeColor.ORANGE.getData() ) );
					}
					if ( list[ 0 ].equalsIgnoreCase( "magenta" ) ) {
						return new ItemStack( Material.INK_SACK, amount, (byte) ( 15 - DyeColor.MAGENTA.getData() ) );
					}
					if ( list[ 0 ].equalsIgnoreCase( "yellow" ) ) {
						return new ItemStack( Material.INK_SACK, amount, (byte) ( 15 - DyeColor.YELLOW.getData() ) );
					}
					if ( list[ 0 ].equalsIgnoreCase( "lime" ) ) {
						return new ItemStack( Material.INK_SACK, amount, (byte) ( 15 - DyeColor.LIME.getData() ) );
					}
					if ( list[ 0 ].equalsIgnoreCase( "pink" ) ) {
						return new ItemStack( Material.INK_SACK, amount, (byte) ( 15 - DyeColor.PINK.getData() ) );
					}
					if ( list[ 0 ].equalsIgnoreCase( "gray" ) ) {
						return new ItemStack( Material.INK_SACK, amount, (byte) ( 15 - DyeColor.GRAY.getData() ) );
					}
					if ( list[ 0 ].equalsIgnoreCase( "cyan" ) ) {
						return new ItemStack( Material.INK_SACK, amount, (byte) ( 15 - DyeColor.CYAN.getData() ) );
					}
					if ( list[ 0 ].equalsIgnoreCase( "purple" ) ) {
						return new ItemStack( Material.INK_SACK, amount, (byte) ( 15 - DyeColor.PURPLE.getData() ) );
					}
					if ( list[ 0 ].equalsIgnoreCase( "blue" ) ) {
						return new ItemStack( Material.INK_SACK, amount, (byte) ( 15 - DyeColor.BLUE.getData() ) );
					}
					if ( list[ 1 ].equalsIgnoreCase( "brown" ) ) {
						return new ItemStack( Material.INK_SACK, amount, (byte) ( 15 - DyeColor.BROWN.getData() ) );
					}
					if ( list[ 1 ].equalsIgnoreCase( "green" ) ) {
						return new ItemStack( Material.INK_SACK, amount, (byte) ( 15 - DyeColor.GREEN.getData() ) );
					}
					if ( list[ 1 ].equalsIgnoreCase( "red" ) ) {
						return new ItemStack( Material.INK_SACK, amount, (byte) ( 15 - DyeColor.RED.getData() ) );
					}
					if ( list[ 1 ].equalsIgnoreCase( "black" ) ) {
						return new ItemStack( Material.INK_SACK, amount, (byte) ( 15 - DyeColor.BLACK.getData() ) );
					}
					return null;
				}
				if ( ( list[ 0 ].equalsIgnoreCase( "coarse" ) ) && ( list[ 1 ].equalsIgnoreCase( "dirt" ) ) ) {
					return new ItemStack( Material.DIRT, amount, (short) 3 );
				}
				if ( list[ 1 ].equalsIgnoreCase( "wood" ) ) {
					if ( list[ 0 ].equalsIgnoreCase( "oak" ) ) {
						return new ItemStack( Material.LOG, amount, (short) 0 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "spruce" ) ) {
						return new ItemStack( Material.LOG, amount, (short) 1 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "birch" ) ) {
						return new ItemStack( Material.LOG, amount, (short) 2 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "jungle" ) ) {
						return new ItemStack( Material.LOG, amount, (short) 3 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "acia" ) ) {
						return new ItemStack( Material.LOG_2, amount );
					}
					return null;
				}
				if ( ( list[ 0 ].equalsIgnoreCase( "sand" ) ) && ( list[ 1 ].equalsIgnoreCase( "sand" ) ) ) {
					return new ItemStack( Material.SAND, amount, (short) 1 );
				}
				if ( list[ 1 ].equalsIgnoreCase( "sandstone" ) ) {
					if ( list[ 0 ].equalsIgnoreCase( "chiseled" ) ) {
						return new ItemStack( Material.SANDSTONE, amount, (short) 1 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "smooth" ) ) {
						return new ItemStack( Material.SANDSTONE, amount, (short) 2 );
					}
					return null;
				}
				if ( ( list[ 0 ].equalsIgnoreCase( "wet" ) ) && ( list[ 1 ].equalsIgnoreCase( "sponge" ) ) ) {
					return new ItemStack( Material.SPONGE, amount, (short) 1 );
				}
				if ( list[ 1 ].equalsIgnoreCase( "sapling" ) ) {
					if ( list[ 0 ].equalsIgnoreCase( "oak" ) ) {
						return new ItemStack( Material.SAPLING, amount );
					}
					if ( list[ 0 ].equalsIgnoreCase( "spruce" ) ) {
						return new ItemStack( Material.SAPLING, amount, (short) 1 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "birch" ) ) {
						return new ItemStack( Material.SAPLING, amount, (short) 2 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "jungle" ) ) {
						return new ItemStack( Material.SAPLING, amount, (short) 3 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "acacia" ) ) {
						return new ItemStack( Material.SAPLING, amount, (short) 4 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "cherrypi" ) ) {
						return new ItemStack( Material.SAPLING, amount );
					}
					if ( list[ 0 ].equalsIgnoreCase( "erythro" ) ) {
						return new ItemStack( Material.SAPLING, amount, (short) 1 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "tororo" ) ) {
						return new ItemStack( Material.SAPLING, amount, (short) 2 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "lavernip" ) ) {
						return new ItemStack( Material.SAPLING, amount, (short) 3 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "caramela" ) ) {
						return new ItemStack( Material.SAPLING, amount, (short) 4 );
					}
					return null;
				}
				if ( ( list[ 0 ].equalsIgnoreCase( "gun" ) ) && ( list[ 1 ].equalsIgnoreCase( "powder" ) ) ) {
					return new ItemStack( Material.SULPHUR, amount );
				}
				if ( ( list[ 0 ].equalsIgnoreCase( "bone" ) ) && ( list[ 1 ].equalsIgnoreCase( "meal" ) ) ) {
					return new ItemStack( Material.INK_SACK, amount, DyeColor.WHITE.getData() );
				}
				if ( ( list[ 0 ].equalsIgnoreCase( "red" ) ) && ( list[ 1 ].equalsIgnoreCase( "sand" ) ) ) {
					return new ItemStack( Material.SAND, amount, (short) 1 );
				}
				if ( list[ 0 ].equalsIgnoreCase( "laser" ) ) {
					if ( list[ 1 ].equalsIgnoreCase( "Charge" ) ) {
						return new ItemStack( Material.ARROW, amount );
					}
					if ( list[ 1 ].equalsIgnoreCase( "gun" ) ) {
						return new ItemStack( Material.BOW, amount );
					}
					return null;
				}
				if ( ( list[ 0 ].equalsIgnoreCase( "chemical" ) ) && ( list[ 1 ].equalsIgnoreCase( "junk" ) ) ) {
					return new ItemStack( Material.PRISMARINE_CRYSTALS, amount );
				}
				if ( list[ 1 ].equalsIgnoreCase( "egg" ) ) {
					if ( list[ 0 ].equalsIgnoreCase( "wolf" ) ) {
						return new ItemStack( Material.MONSTER_EGG, amount, (short) 95 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "pig" ) ) {
						return new ItemStack( Material.MONSTER_EGG, amount, (short) 90 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "horse" ) ) {
						return new ItemStack( Material.MONSTER_EGG, amount, (short) 100 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "chicken" ) ) {
						return new ItemStack( Material.MONSTER_EGG, amount, (short) 93 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "rabbit" ) ) {
						return new ItemStack( Material.MONSTER_EGG, amount, (short) 101 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "Cow" ) ) {
						return new ItemStack( Material.MONSTER_EGG, amount, (short) 92 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "sheep" ) ) {
						return new ItemStack( Material.MONSTER_EGG, amount, (short) 91 );
					}
					return null;
				}
				return null;
			}
			if ( list.length == 3 ) {
				if ( ( list[ 2 ].equalsIgnoreCase( "planks" ) ) && ( list[ 1 ].equalsIgnoreCase( "wood" ) ) ) {
					if ( list[ 0 ].equalsIgnoreCase( "oak" ) ) {
						return new ItemStack( Material.WOOD_PLATE, amount, (short) 0 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "spruce" ) ) {
						return new ItemStack( Material.WOOD_PLATE, amount, (short) 1 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "birch" ) ) {
						return new ItemStack( Material.WOOD_PLATE, amount, (short) 2 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "jungle" ) ) {
						return new ItemStack( Material.WOOD_PLATE, amount, (short) 3 );
					}
					if ( list[ 0 ].equalsIgnoreCase( "acia" ) ) {
						return new ItemStack( Material.WOOD_PLATE, amount, (short) 4 );
					}
					return null;
				}
				if ( ( list[ 0 ].equalsIgnoreCase( "dark" ) ) && ( list[ 1 ].equalsIgnoreCase( "oak" ) ) && ( list[ 2 ].equalsIgnoreCase( "sapling" ) ) ) {
					return new ItemStack( Material.SAPLING, amount, (short) 5 );
				}
				if ( ( list[ 0 ].equalsIgnoreCase( "special" ) ) && ( list[ 1 ].equalsIgnoreCase( "helmet" ) ) ) {
					if ( list[ 2 ].equalsIgnoreCase( "1" ) ) {
						return new ItemStack( Material.MYCEL, amount );
					}
					if ( list[ 2 ].equalsIgnoreCase( "2" ) ) {
						return new ItemStack( Material.DIRT, amount, (short) 2 );
					}
					if ( list[ 2 ].equalsIgnoreCase( "3" ) ) {
						return new ItemStack( Material.BEDROCK, amount );
					}
					if ( list[ 2 ].equalsIgnoreCase( "4" ) ) {
						return new ItemStack( Material.COMMAND, amount );
					}
				}
				return null;
			}
		}
		return null;
	}

	public CustomConfig getCfg()
	{
		return this.cfg;
	}

	public Config getItemConfig()
	{
		return this.itemConfig;
	}
}
