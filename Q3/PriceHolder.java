package Question3;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public final class PriceHolder {
	private final Map<String, Price> prices = new ConcurrentHashMap<>();
	private static class Price {
		BigDecimal current;
		boolean viewed;
	}

	private final ConcurrentHashMap<String, BigDecimal> queue = new ConcurrentHashMap<>();

	private void updatePrice(String priceName) {
		Price price = getEntity(priceName);
		synchronized(price) {
			BigDecimal queued = queue.remove(priceName);
			if (queued != null && !queued.equals(price.current)) {
				//Long processing etc goes here
				price.current = queued;
				price.viewed = false;
				price.notifyAll();
			}
		}
	}

	public PriceHolder() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				ExecutorService processor = Executors.newFixedThreadPool(2);
				while (true) {
					List<Callable<Void>> tasks = new LinkedList<>();
					for (final String priceName : queue.keySet()) {
						tasks.add(new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								updatePrice(priceName);
								return null;
							}
						});
					}
					try {
						List<Future<Void>> futures = processor.invokeAll(tasks);
						for (Future<Void> future: futures) {
							future.get();
						}
					} catch (InterruptedException | ExecutionException e) {
						System.err.println(e);
					}
					synchronized(queue){
						if(queue.isEmpty()){
							try {
								queue.wait();
							} catch (InterruptedException e) {
								System.err.println(e);
							}
						}
					}
					
				}

			}
		}).start();
	}

	private Price getEntity(String e) {
		Price price = prices.get(e);
		if (price != null) {
			return price;
		} else {// assume a new price name is rare so default action is read and return without locking
			synchronized (this) {
				price = prices.get(e);
				if (price == null) {// price was not created in the meantime
					price = new Price();
					prices.put(e, price);
				}
			}
		}
		return price;
	}

	/** Called when a price ‘p’ is received for an entity ‘e’ */
	public void putPrice(String e, BigDecimal p) {
		System.out.println(System.currentTimeMillis() + " Queued: " + e + " = " + p);
		synchronized(queue){
			queue.put(e, p);
			queue.notifyAll();
		}
		
	}

	/** Called to get the latest price for entity ‘e’ */
	public BigDecimal getPrice(String e) {
		Price price = getEntity(e);
		synchronized(price) {
			updatePrice(e);
			price.viewed = true;
			return price.current;
		}
	}

	/**
	 * Called to determine if the price for entity ‘e’ has changed since the
	 * last call to getPrice(e).
	 */
	public boolean hasPriceChanged(String e) {
		Price price = getEntity(e);
		synchronized(price) {
			updatePrice(e);
			return !price.viewed;
		}
		
	}

	/**
	 * Returns the next price for entity ‘e’. If the price has changed since the last
	 * call to getPrice() or waitForNextPrice(), it returns immediately that price.
	 * Otherwise it blocks until the next price change for entity ‘e’.
	 */
	public BigDecimal waitForNextPrice(String e) throws InterruptedException {
		Price price = getEntity(e);
		synchronized(price) {
			if (price.viewed) {
				price.wait();
			}
			return getPrice(e);
		}
	}


	public static void main(String[] args){
		final PriceHolder phe = new PriceHolder();
		phe.putPrice("a", new BigDecimal(12));

		Thread thread1 = new Thread() {
			public void run() {
				System.out.println(phe.getPrice("a"));
				phe.putPrice("a", new BigDecimal(4));
				try {
					System.out.println(phe.waitForNextPrice("a"));
					System.out.println(phe.waitForNextPrice("a"));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		Thread thread2 = new Thread() {
			public void run() {
				try {
					for (int i = 15; i < 20; i++) {
						sleep(5000);
						phe.putPrice("a", new BigDecimal(i));
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		thread1.start();
		thread2.start();
	}

}
