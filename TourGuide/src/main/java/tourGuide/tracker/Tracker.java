package tourGuide.tracker;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tourGuide.service.TourGuideService;
import tourGuide.user.User;

@Service
public class Tracker extends Thread {
	private Logger logger = LoggerFactory.getLogger(Tracker.class);
	private static final long trackingPollingInterval = TimeUnit.MINUTES.toSeconds(5);
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final TourGuideService tourGuideService;
	private boolean stop = false;

	public Tracker(TourGuideService tourGuideService) {
		this.tourGuideService = tourGuideService;
		
		executorService.submit(this);
		addShutDownHook();
	}
	
	/**
	 * Assures to shut down the Tracker thread
	 */	
	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() { 
			@Override
			public void run() {
				stop = true;
				executorService.shutdownNow();
			} 
		}); 
	}
	
	@Override
	public void run() {
		StopWatch stopWatch = new StopWatch();
			while(true) {
				if(Thread.currentThread().isInterrupted() || stop) {
					logger.debug("Tracker stopping");
					break;
				}
				
				List<User> users = tourGuideService.getAllUsers();
				logger.debug("Begin Tracker. Tracking " + users.size() + " users.");
				stopWatch.start();
				CompletableFuture<?>[] futures = users.parallelStream().map(
						u -> tourGuideService.trackUserLocation(u))
						.toArray(CompletableFuture[]::new);
				CompletableFuture.allOf(futures).join();
				
				stopWatch.stop();
				logger.debug("Tracker Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds."); 
				stopWatch.reset();
				try {
					logger.debug("Tracker sleeping");
					TimeUnit.SECONDS.sleep(trackingPollingInterval);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
		
}
