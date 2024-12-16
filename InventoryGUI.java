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

        // Item list
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
        inventoryPanel = new BagPanel(inventory, inventory, null, chest);
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

        // predefine window size so you don't have to stretch the window
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
            // Handle stackable items - this should be adjusted for proper stacking
            if (item.isStackable()) {
                chest.addItem(new Item(item.getName(), true));
            }
            SwingUtilities.invokeLater(() -> chestPanel.updateSlots());
        }
        chestPanel.updateSlots();
    }

    private void clearInventory() {
        inventory.clear();
        inventoryPanel.updateSlots();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(InventoryGUI::new);
    }

    class Bag {
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

    class BagPanel extends JPanel {
        private Bag bag;
        private Bag inventory;
        private BagPanel inventoryPanel;
        private Bag chest;

        public BagPanel(Bag bag, Bag inventory, BagPanel inventoryPanel, Bag chest) {
            this.bag = bag;
            this.inventory = inventory;
            this.inventoryPanel = inventoryPanel;
            this.chest = chest;
            setLayout(new GridLayout(4, 3));
            setBorder(BorderFactory.createTitledBorder(bag.getCapacity() == 12 ? "Inventory" : "Chest"));
            updateSlots();
        }

        public void updateSlots() {
            removeAll();
            for (Item item : bag.getItems()) {
                ItemSlot slot = new ItemSlot(item);
                add(slot);

                slot.setTransferHandler(new TransferHandler() {
                    @Override
                    public int getSourceActions(JComponent c) {
                        return COPY_OR_MOVE;
                    }

                    @Override
                    protected Transferable createTransferable(JComponent c) {
                        return new ItemTransferable(((ItemSlot) c).getItem());
                    }

                    @Override
                    protected void exportDone(JComponent source, Transferable data, int action) {
                        if (action == MOVE) {
                            ItemSlot itemSlot = (ItemSlot) source;
                            bag.removeItem(itemSlot.getItem());
                            itemSlot.setItem(null);
                            source.revalidate();
                            source.repaint();
                        }
                    }
                });

                slot.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (slot.getItem() != null) {
                            TransferHandler handler = slot.getTransferHandler();
                            handler.exportAsDrag(slot, e, TransferHandler.COPY_OR_MOVE);
                        }
                    }
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2 && slot.getItem() != null) {
                            if (bag == inventory) {
                                // If this is the inventory bag, remove the item
                                bag.removeItem(slot.getItem());
                                slot.setItem(null);
                                updateSlots(); // Refresh the inventory slots
                            } else {
                                // If this is the chest, try to move the item to the inventory
                                if (inventory.addItem(slot.getItem())) {
                                    bag.removeItem(slot.getItem());
                                    slot.setItem(null);
                                    slot.updateDisplay();
                                    if (inventoryPanel != null) inventoryPanel.updateSlots();
                                }
                            }
                        }
                    }
                });
            }

            int emptySlots = bag.getCapacity() - bag.getItems().size();
            for (int i = 0; i < emptySlots; i++) {
                ItemSlot slot = new ItemSlot();
                slot.setTransferHandler(new TransferHandler() {
                    @Override
                    public boolean canImport(TransferSupport support) {
                        return support.isDataFlavorSupported(ItemTransferable.ITEM_FLAVOR);
                    }

                    @Override
                    public boolean importData(TransferSupport support) {
                        if (!canImport(support)) {
                            return false;
                        }
                        try {
                            Item newItem = (Item) support.getTransferable().getTransferData(ItemTransferable.ITEM_FLAVOR);
                            if (bag.addItem(newItem)) {
                                slot.setItem(newItem);
                                revalidate();
                                repaint();
                                return true;
                            }
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
            setTransferHandler(new TransferHandler() {
                @Override
                public boolean canImport(TransferSupport support) {
                    return support.isDrop() && support.isDataFlavorSupported(ItemTransferable.ITEM_FLAVOR)
                            && ItemSlot.this.item == null;
                }

                @Override
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

        protected void updateDisplay() {
            setText(item != null ? item.getName() : "");
        }

        public Item getItem() {
            return item;
        }

        public void setItem(Item newItem) {
            this.item = newItem;
            updateDisplay();
            revalidate();
            repaint();
        }
    }
}

class ItemTransferable implements Transferable {
    public static final DataFlavor ITEM_FLAVOR = new DataFlavor(Item.class, "Item");
    private Item item;

    public ItemTransferable(Item item) {
        this.item = item;
    }

    public ItemTransferable(InventoryGUI.Item item) {
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
