package ro.ubb.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * The shop which manages the access for the items in the storage,
 * each desired item has a lock associated to it so that at one given time
 * only one customer can access it.
 * The shop also has a "global" lock, which behaves as a readwrite lock,
 * where it is used to lock down the whole shop to perform the inventory check,
 * and no to interrupt the other customers, only to make them wait.
 */
public class Shop {

    private final HashMap<Product, Integer> products;
    private final HashMap<Product, Integer> initialProductList;
    /**
     * Each item has its own mutex, so concurrent access to other items is allowed, while if a user wants the same
     * product it has to wait
     */
    private final HashMap<Product, ReentrantLock> locksForProducts;

    private final ReadWriteLock shopLock = new ReentrantReadWriteLock();

    double totalIncome = 0;
    List<Bill> productsBought = new ArrayList<>();

    public Shop() {
        products = new HashMap<>();
        initialProductList = new HashMap<>();
        locksForProducts = new HashMap<>();
    }

    public List<Bill> getProductsBought() {
        return productsBought;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public HashMap<Product, Integer> getInitialProductList() {
        return initialProductList;
    }

    /**
     * The customer adds a bill to the shop
     * Only one customer can add a bill at a given time
     * and locks this method from other customers
     * Only on bill is added, and after the operation the bill is part of the list of bills and the money is increased
     * by the amount that is on the bill
     * @param billToAdd the bill to add
     */
    public synchronized void addBill(Bill billToAdd) {
        shopLock.readLock().lock();
        productsBought.add(billToAdd);
        totalIncome += billToAdd.getTotalAmount();
        shopLock.readLock().unlock();
    }

    /**
     * Lock the shop in exclusive mode to verify it
     */
    public void lockShop() {
        shopLock.writeLock().lock();
    }

    /**
     * Unlock the shop in exclusive mode to verify it
     */
    public void unlockShop() {
        shopLock.writeLock().unlock();
    }

    public HashMap<Product, Integer> getProducts() {
        return products;
    }

    public List<Product> getProductNames() {
        return new ArrayList<>(products.keySet());
    }

    /**
     * The items are added at the initial phase of the applciation
     *
     * @param product the product to add
     * @param amount  the quantity present
     */
    public void addAProductWithAmount(Product product, int amount) {

        if (amount < 0)
            throw new RuntimeException("Invalid, can't have negative amount");
        if (this.products.containsKey(product)) {
            this.products.put(product, this.products.get(product) + amount);
            this.initialProductList.put(product, this.products.get(product) + amount);
        } else {
            this.products.put(product, amount);
            this.initialProductList.put(product, amount);
            this.locksForProducts.put(product, new ReentrantLock());
        }
    }

    /**
     * To remove a given amount of a certain product
     *
     * @param product the given product
     * @param amount  the amount to be removed
     * @throws NotEnoughProductsException if the customer wants to remove more items than there are items
     */
    public void removeAProductWithAmount(Product product, int amount) throws NotEnoughProductsException {

        if (amount < 0)
            throw new RuntimeException("Invalid, can't have negative amount");
        if (!this.products.containsKey(product))
            throw new RuntimeException("No item of that name!");

        /** The customer requests access to the shop, if it is not under verification stage by the verifier
         * The invariant is that the other items are on modified by this thread and the shop readlock (shared mode) is released,
         * and the item lock is released. In case the customer obtains the item the quantity of th eproduct is decreased
         *  I use the readlock (shared mode) in order to make the acces for the shoppers and deny the storage verification to verify the sotrage
         *  while someone is shopping
         * */
        shopLock.readLock().lock();
        /**
         * The specific product lock
         */
        ReentrantLock mutexForItem = locksForProducts.get(product);

        mutexForItem.lock();
        try {
            if (this.products.get(product) < amount) {
                throw new NotEnoughProductsException("You are requesting too many items");
            } else {
                this.products.replace(product, this.products.get(product) - amount);
            }
        } finally {
            mutexForItem.unlock();
            shopLock.readLock().unlock();
        }

    }

}
