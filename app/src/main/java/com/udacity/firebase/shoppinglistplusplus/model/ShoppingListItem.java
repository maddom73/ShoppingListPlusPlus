package com.udacity.firebase.shoppinglistplusplus.model;

/**
 * Defines the data structure for ShoppingListItem objects.
 */
public class ShoppingListItem {
    private String itemName;
    private String owner;
    private String boughtBy;
    private String imagePath;
    private boolean bought;

    /**
     * Required public constructor
     */
    public ShoppingListItem() {
    }

    /**
     * Use this constructor to create new ShoppingListItem.
     * Takes shopping list item name and list item owner email as params
     *
     * @param itemName
     * @param owner
     */
    public ShoppingListItem(String itemName, String owner, String imagePath) {
        this.itemName = itemName;
        this.owner = owner;
        this.imagePath = imagePath;
        this.boughtBy = null;
        this.bought = false;

    }

    public String getItemName() { return itemName; }

    public String getOwner() {
        return owner;
    }

    public String getBoughtBy() {
        return boughtBy;
    }
    public String getImagePath() {
        return imagePath;
    }
    public boolean isBought() {
        return bought;
    }

}
