import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class InventoryGUI extends JFrame {

    private Bag inventory;
    private Bag chest;
    private BagPanel inventoryPanel;
    private BagPanel chestPanel;
    private JButton openChestButton;
    private JButton clearInventoryButton;

    private List<Item> items = new ArrayList<>();

    public InventoryGUI() {
        super("Hero Bag");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

       //Item list
        items.add(new Item("Sword", false));
        items.add(new Item("Hat", false));
        items.add(new Item("Cloak", false));
        items.add(new Item("Boots", false));
        items.add(new Item("Mana Potion", true));
        items.add(new Item("Health Potion", true));

        // Define bags sizes
        inventory = new Bag(12);
        chest = new Bag(3);

        // Bag panels
        inventoryPanel = new BagPanel(inventory, inventory, inventoryPanel, chest);
        chestPanel = new BagPanel(chest, inventory, inventoryPanel, chest);

        // Buttons
        openChestButton = new JButton("Open Chest");
        openChestButton.addActionListener(e -> openChest());

        clearInventoryButton = new JButton("Clear Inventory");
        clearInventoryButton.addActionListener(e -> clearInventory());

        // Put bags in window
        add(inventoryPanel, BorderLayout.CENTER);
        add(chestPanel, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(openChestButton);
        buttonPanel.add(clearInventoryButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // predefine window size so you dont have to stretch the window
        setSize(400, 300);
        setMinimumSize(new Dimension(400, 300));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void openChest() {
        chest.clear();
        Random random = new Random();
        List<Item> tempItems = new ArrayList<>(items);

        for (int i = 0; i < chest.getCapacity() && !tempItems.isEmpty(); i++) {
            int randomIndex = random.nextInt(tempItems.size());
            Item item = tempItems.remove(randomIndex);
            chest.addItem(item);
            // Handle stackable items
            if (item.isStackable()) {
                chest.addItem(new Item(item.getName(), true));
            }
            SwingUtilities.invokeLater(() -> chestPanel.updateSlots());
        }
        chestPanel.updateSlots();
    }
    //Deletes the inventory
    private void clearInventory() {
        inventory.clear();
        inventoryPanel.updateSlots();
    }
    //main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(InventoryGUI::new);
    }
}
    //bag constructor for main inventory and chest
class Bag {//this is the bag ADT for the bag and the chest
    private int capacity;
    private List<Item> items = new ArrayList<>();

    public Bag(int capacity) {
        this.capacity = capacity;
    }

    public boolean addItem(Item item) {
        if (items.size() < capacity) {
            items.add(item);
            return true;
        }
        return false;
    }

    public void removeItem(Item item) {
        items.remove(item);
    }

    public void clear() {
        items.clear();
    }

    public List<Item> getItems() {
        return new ArrayList<>(items);
    }

    public int getCapacity() {
        return capacity;
    }
}
//placeholder for the gui elements of the bags
class BagPanel extends JPanel {
    private Bag bag;
    private Bag inventory;
    private BagPanel inventoryPanel;
    private Bag chest;

    //gui for the main bag
    public BagPanel(Bag bag, Bag inventory, BagPanel inventoryPanel, Bag chest) {
        this.bag = bag;
        this.inventory = inventory;
        this.inventoryPanel = inventoryPanel;
        this.chest = chest;
        setLayout(new GridLayout(4, 3));
        setBorder(BorderFactory.createTitledBorder(bag.getCapacity() == 12 ? "Inventory" : "Chest"));
        updateSlots();
    }


    //This is a horrible fight clicking and dragging to populate the
    //main back is exceedingly difficult
    public void updateSlots() {
        removeAll();
        for (Item item : bag.getItems()) {
            ItemSlot slot = new ItemSlot(item);
            add(slot);

            // Doesnt work. Doubleclick to add items
            slot.setTransferHandler(new TransferHandler() {
                @Override
                public int getSourceActions(JComponent c) {
                    return COPY_OR_MOVE;
                }

                @Override
                protected Transferable createTransferable(JComponent c) {
                    return new ItemTransferable(slot.getItem());
                }

                @Override
                protected void exportDone(JComponent source, Transferable data, int action) {
                    if (action == MOVE) {
                        bag.removeItem(slot.getItem());
                        slot.setItem(null);
                        source.revalidate();
                        source.repaint();
                    }
                }
            });
            //this is supposed to allow click and drag
            slot.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (slot.getItem() != null) {
                        TransferHandler handler = slot.getTransferHandler();
                        handler.exportAsDrag(slot, e, TransferHandler.COPY_OR_MOVE);
                    }
                }
            });

            // doubleclick funcionality is all that works
            if (bag == this.chest) {
                slot.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2 && slot.getItem() != null) {
                            if (inventory.addItem(slot.getItem())) {
                                bag.removeItem(slot.getItem());
                                slot.setItem(null);
                                slot.updateDisplay();
                                if (inventoryPanel != null) inventoryPanel.updateSlots();
                            }
                        }
                    }
                });
            }
        }

        // This rerferences back to capacity to build the bags
        int emptySlots = bag.getCapacity() - bag.getItems().size();
        for (int i = 0; i < emptySlots; i++) {
            ItemSlot slot = new ItemSlot();
            slot.setTransferHandler(new TransferHandler() {
                @Override//allows the bag to accept items
                public boolean canImport(TransferSupport support) {
                    return support.isDataFlavorSupported(ItemTransferable.ITEM_FLAVOR);
                }

                @Override//actually accepts the items
                public boolean importData(TransferSupport support) {
                    if (!canImport(support)) {
                        return false;
                    }
                    try {//changes the slot from emtpy to something with something in it
                        Item newItem = (Item) support.getTransferable().getTransferData(ItemTransferable.ITEM_FLAVOR);
                        if (bag.addItem(newItem)) {
                            slot.setItem(newItem);
                            revalidate();
                            repaint();
                            return true;
                        }//if I try to put something in the bag that doesn't exist
                        //this throws the exception for that
                        //but it has to be here to close the try
                        //which paints the itemslot after you take it out of the
                        //chest and put it in the inventory
                    } catch (UnsupportedFlavorException | IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });
            add(slot);
        }
        revalidate();
        repaint();
    }
}
//This is where the gui words of list items are made graphical elements
class ItemSlot extends JLabel {
    private Item item;

    public ItemSlot() {
        this(null);
    }

    public ItemSlot(Item item) {
        this.item = item;
        updateDisplay();
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
        setHorizontalAlignment(CENTER);
        setFocusable(true);
        //this says the items are click and draggable
        //but this doenst work
        setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDrop() && support.isDataFlavorSupported(ItemTransferable.ITEM_FLAVOR)
                        && ItemSlot.this.item == null;
            }

            @Override//I think the problem for click and drag is this
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                try {
                    Item newItem = (Item) support.getTransferable().getTransferData(ItemTransferable.ITEM_FLAVOR);
                    ItemSlot.this.item = newItem;
                    updateDisplay();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
    }
    //redoes the window after it changes
    protected void updateDisplay() {
        setText(item != null ? item.getName() : "");
    }
    //Tells that item is here declares the place to put it for the transfer
    //method intellij put this here for me
    public Item getItem() {
        return item;
    }
    //calls the functionality of update display to
    //actually update the display
    public void setItem(Item newItem) {
        this.item = newItem;
        updateDisplay();
        revalidate();
        repaint();
    }
}
//This is part of you're supposed to be able to stack potions
//somehow I gotta put a number in there I'll figure it out
//Im not done with this
class Item {
    private String name;
    private boolean stackable;

    public Item(String name, boolean stackable) {
        this.name = name;
        this.stackable = stackable;
    }

    public String getName() {
        return name;
    }

    public boolean isStackable() {
        return stackable;
    }
}
//
class ItemTransferable implements Transferable {//intellij made the Transferable.java class
    public static final DataFlavor ITEM_FLAVOR = new DataFlavor(Item.class, "Item");
    private Item item;

    public ItemTransferable(Item item) {//right here intellij is calling for
        this.item = item;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{ITEM_FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(ITEM_FLAVOR);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor.equals(ITEM_FLAVOR)) {
            return item;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
}