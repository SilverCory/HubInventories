package me.datdenkikniet.ItemDinges;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class InventoryItem
{
	private final List<String> commands;
	private final String name;
	private final ItemStack stack;
	private final int position;

	public InventoryItem( ItemDingesMain pl, int pos, Material mate, int amounte, List<String> loree, String namee, List<String> cmds, short dmgValue )
	{

		// init these fields.
		this.position = pos;
		this.name = namee;
		this.commands = cmds;

		this.stack = new ItemStack( mate, amounte );
		this.stack.setDurability( dmgValue );

		ItemMeta meta = this.stack.getItemMeta();

		// Set the item name.
		meta.setDisplayName( this.name );

		// Add the lore.
		if ( ( loree != null ) && ( !loree.isEmpty() ) ) {
			for ( String s : loree ) {
				String sp = ChatColor.translateAlternateColorCodes( '&', s );
				loree.set( loree.indexOf( s ), sp );
			}
			meta.setLore( loree );
		}

		// Apply the meta.
		this.stack.setItemMeta( meta );

		// Add this to the main class.
		pl.addItem( this );

	}

	public ItemStack getStack()
	{
		return this.stack;
	}

	public int getPosition()
	{
		return this.position;
	}

	public List<String> getCommands()
	{
		return this.commands;
	}

	public String getName()
	{
		return this.name;
	}
}
