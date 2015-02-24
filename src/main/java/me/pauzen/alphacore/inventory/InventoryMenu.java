/*
 *  Created by Filip P. on 2/20/15 7:01 PM.
 */

package me.pauzen.alphacore.inventory;

import me.pauzen.alphacore.inventory.elements.Element;
import me.pauzen.alphacore.inventory.elements.InteractableElement;
import me.pauzen.alphacore.inventory.elements.ToggleableElement;
import me.pauzen.alphacore.inventory.misc.ClickType;
import me.pauzen.alphacore.inventory.misc.Coordinate;
import me.pauzen.alphacore.inventory.misc.ElementGetter;
import me.pauzen.alphacore.players.CorePlayer;
import me.pauzen.alphacore.utils.InvisibleID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Predicate;

public abstract class InventoryMenu {

    private Map<Coordinate, Element> elementMap = new HashMap<>();

    private Map<Coordinate, Predicate<InventoryClickEvent>> allowedClicking = new HashMap<>();

    private InvisibleID inventoryID;

    private Map<UUID, Inventory> openedInventories = new HashMap<>();

    private String name;
    private int    size;

    /**
     * Creates new Inventory with null holder, size and name as specified with the name getting an invisible unique ID appended to the end of it.
     *
     * @param name The desired name of the inventory.
     * @param size The desired size of the inventory, must be a multiple of 9. (size % 9 == 0) condition must be met.
     */
    public InventoryMenu(String name, int rows) {
        inventoryID = InvisibleID.generate();
        registerElements();
        fillRemaining();
        InventoryManager.getManager().registerMenu(this);
        this.name = name;
        this.size = rows * 9;
    }
    
    public void openInventory(Player player, Inventory inventory) {
        this.openedInventories.put(player.getUniqueId(), inventory);
    }
    
    public void closeInventory(Player player) {
        this.openedInventories.remove(player.getUniqueId());
    }

    public Inventory generateInventory(Player player) {

        Inventory inventory = Bukkit.createInventory(null, size, name + inventoryID.getId());

        elementMap.entrySet().forEach(entry -> {

            int i = entry.getKey().toSlot();

            if (entry.getValue() instanceof ToggleableElement) {
                ToggleableElement toggleableElement = (ToggleableElement) entry.getValue();

                toggleableElement.testPredicate(CorePlayer.get(player), inventory);
            }
            inventory.setItem(i, entry.getValue().getRepresentation());
        });

        return inventory;
    }

    /**
     * Processes InventoryClickEvent and looks for any interactable elements that the player clicked.
     *
     * @param event InventoryClickEvent to process.
     */
    public final void process(InventoryClickEvent event) {

        Coordinate clicked = toCoordinate(event.getRawSlot());

        Element clickedElement = elementMap.get(clicked);

        if (clickedElement instanceof InteractableElement) {
            InteractableElement interactableElement = (InteractableElement) clickedElement;

            interactableElement.onClick((Player) event.getWhoClicked(), event.getAction() == InventoryAction.PICKUP_ALL ? ClickType.INVENTORY_LEFT :
                    event.getAction() == InventoryAction.PICKUP_HALF ? ClickType.INVENTORY_RIGHT : ClickType.OTHER, event.getInventory());
        }
        if (!shouldAllowClick(event)) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }

    /**
     * Returns whether the InventoryClickEvent should be cancelled or not.
     *
     * @param e The InventoryClickEvent to get the outcome of.
     * @return Whether or not to cancel the event.
     */
    public boolean shouldAllowClick(InventoryClickEvent e) {

        if (e.getRawSlot() >= size) {
            return true;
        }

        Coordinate inventoryCoordinate = toCoordinate(e.getRawSlot());

        Predicate<InventoryClickEvent> inventoryClickEventPredicate = allowedClicking.get(inventoryCoordinate);

        return inventoryClickEventPredicate != null && inventoryClickEventPredicate.test(e);
    }

    /**
     * Where to register elements and such.
     */
    public abstract void registerElements();

    /**
     * Triggered when a player opens an inventory.
     * @param corePlayer Player that opened the inventory.
     */
    public void onOpen(CorePlayer corePlayer) {}

    /**
     * Fills unregistered element slots with blank elements.
     */
    public void fillRemaining() {
        for (int i = 0; i < size; i++) {

            Coordinate coordinate = toCoordinate(i);

            elementMap.putIfAbsent(coordinate, Element.BLANK_ELEMENT);
        }
    }

    /**
     * Shows the inventory to a player.
     *
     * @param player Player to show the inventory to.
     */
    public void show(Player player) {
        Inventory inventory = generateInventory(player);
        player.openInventory(inventory);
        openInventory(player, inventory);
    }

    /**
     * Shows the inventory to a player.
     *
     * @param corePlayer Player to show the inventory to.
     */
    public void show(CorePlayer corePlayer) {
        show(corePlayer.getPlayer());
    }

    /**
     * Updates an element in an already open inventory.
     *
     * @param coordinate Where to execute the update.
     * @param item       What to update the item to.
     */
    public void updateElement(Inventory inventory, Coordinate coordinate, ItemStack item) {
        inventory.setItem(coordinate.toSlot(), item);
    }

    /**
     * Updates an element in an already open inventory.
     *
     * @param coordinate Where to execute the update.
     */
    public void updateElement(Inventory inventory, Coordinate coordinate) {
        updateElement(inventory, coordinate, elementMap.get(coordinate).getRepresentation());
    }

    /**
     * Creates a condition that must be met if shouldAllowClick were to return true.
     *
     * @param inventoryCoordinate The coordinates where the allowance condition should be checking.
     * @param predicate           The condition that must be met.
     */
    public void createAllowanceConditionForClickingAt(Coordinate inventoryCoordinate, Predicate<InventoryClickEvent> predicate) {
        allowedClicking.put(inventoryCoordinate, predicate);
    }

    /**
     * Creates a condition that must be met if shouldAllowClick were to return true.
     *
     * @param predicate The condition that must be met.
     */
    public void createAllowanceConditionForClickingAt(int x, int y, Predicate<InventoryClickEvent> predicate) {
        createAllowanceConditionForClickingAt(new Coordinate(x, y), predicate);
    }
    
    public void createAllowanceConditionForClickingAt(int x, int y) {
        createAllowanceConditionForClickingAt(x, y, (clickEvent) -> true);
    }

    /**
     * Gets all elements surrounding a coordinate.
     *
     * @param coordinate The coordinate to get the elements around.
     * @return The found elements.
     */
    public Element[] getElementsAround(Coordinate coordinate) {
        Element[] foundElements = new Element[8];
        for (int i = 0; i < Coordinate.Direction.values().length; i++) {
            foundElements[i] = getElementAt(Coordinate.Direction.values()[i].getRelative(coordinate));
        }
        return foundElements;
    }
    
    public Element[] getElementsBetween(int x1, int y1, int x2, int y2) {
        
        Element[] elements = new Element[size];
        
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);

        int slot = 0;
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                elements[slot] = getElementAt(x, y);
                slot++;
            }
        }
        
        return elements;
    }

    private static Coordinate toCoordinate(int x, int y) {
        return Coordinate.coordinate(x, y);
    }

    private static Coordinate toCoordinate(int inventorySlot) {
        return Coordinate.fromSlot(inventorySlot);
    }

    public String getName() {
        return name;
    }

    public String getID() {
        return inventoryID.getId();
    }

    public Element getElementAt(Coordinate coordinate) {
        return elementMap.get(coordinate);
    }

    public Element getElementAt(int x, int y) {
        return elementMap.get(new Coordinate(x, y));
    }
    
    public void setTypeAt(int x, int y, Material type) {
        setElementAt(x, y, new Element(type));
    }
    
    public void setItemAt(int x, int y, ItemStack itemStack) {
        setElementAt(x, y, new Element(itemStack));
    }
    
    public void setElementsBetween(int x1, int y1, int x2, int y2, ElementGetter elementGetter) {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                setElementAt(x, y, elementGetter.getElement(Coordinate.coordinate(x, y)));
            }
        }
    }
    
    public void setElementsBetween(Coordinate coordinate1, Coordinate coordinate2, ElementGetter elementGetter) {
        setElementsBetween(coordinate1.getX(), coordinate1.getY(), coordinate2.getX(), coordinate2.getY(), elementGetter);
    }
    
    public ItemStack getItemAt(int x, int y) {
        Element element = getElementAt(x, y);
        if (element == null) {
            return new ItemStack(Material.AIR);
        }
        return element.getRepresentation();
    }

    public Material getTypeAt(int x, int y) {
        return getItemAt(x, y).getType();
    }

    public ItemStack getItemAt(Inventory inventory, int x, int y) {
        return inventory.getItem(Coordinate.asSlot(x, y));
    }

    public Material getTypeAt(Inventory inventory, int x, int y) {
        return getItemAt(inventory, x, y).getType();
    }

    public ItemStack getItemAt(Inventory inventory, Coordinate coordinate) {
        return getItemAt(inventory, coordinate.getX(), coordinate.getY());
    }

    public Material getTypeAt(Inventory inventory, Coordinate coordinate) {
        return getItemAt(inventory, coordinate).getType();
    }
    
    public void setElementAt(Coordinate coordinate, Element element) {
        this.elementMap.put(coordinate, element);
    }

    public void setElementAt(int x, int y, Element element) {
        this.elementMap.put(new Coordinate(x, y), element);
    }

    public List<Element> getElements() {
        
        List<Element> elements = new ArrayList<>();
        
        elementMap.entrySet().stream().map(Map.Entry::getValue).forEach(elements::add);
        
        return elements;
    }

    public Collection<Inventory> getOpen() {
        return openedInventories.values();
    }
}
