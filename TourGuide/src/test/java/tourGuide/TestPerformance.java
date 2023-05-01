package tourGuide;

import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Before;
import org.junit.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.helper.InternalTestHelper;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;

public class TestPerformance {
	
	/*
	 * A note on performance improvements:
	 *     
	 *     The number of users generated for the high volume tests can be easily adjusted via this method:
	 *     
	 *     		InternalTestHelper.setInternalUserNumber(100000);
	 *     
	 *     
	 *     These tests can be modified to suit new solutions, just as long as the performance metrics
	 *     at the end of the tests remains consistent. 
	 * 
	 *     These are performance metrics that we are trying to hit:
	 *     
	 *     highVolumeTrackLocation: 100,000 users within 15 minutes:
	 *     		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
     *
     *     highVolumeGetRewards: 100,000 users within 20 minutes:
	 *          assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 */
	
	private TourGuideModule tourGuideModule;
	private GpsUtil gpsUtil;
	private RewardCentral rewardCentral;
	private RewardsService rewardsService;
	private ExecutorService executorService;
	private TourGuideService tourGuideService;
	private List<User> allUsers;
	private StopWatch stopWatch ;
	
	@Before
	public void setUp() {
		tourGuideModule = new TourGuideModule();
		gpsUtil = tourGuideModule.getGpsUtil();
		rewardCentral = tourGuideModule.getRewardCentral();
		rewardsService = tourGuideModule.getRewardsService();
		executorService = tourGuideModule.getExecutorService();
		InternalTestHelper.setInternalUserNumber(1000);
		tourGuideService = new TourGuideService(gpsUtil, rewardsService, rewardCentral, executorService);
		allUsers = tourGuideService.getAllUsers();
		
	}
	
	@Test(expected = CompletionException.class)
	public void highVolumeTrackLocation() throws InterruptedException, ExecutionException {
		// Users should be incremented up to 100,000, and test finishes within 15 minutes
		stopWatch = new StopWatch();
		stopWatch.start();

		CompletableFuture<?>[] futures = allUsers.parallelStream()
					.map(user -> tourGuideService.trackUserLocation(user))
					.toArray(CompletableFuture[]::new);
		CompletableFuture.allOf(futures).join();
		
		stopWatch.stop();

		System.out.println("highVolumeTrackLocation: Time Elapsed: " + 
				TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds."); 
		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}
	
	@Test
	public void highVolumeGetRewards() {
		// Users should be incremented up to 100,000, and test finishes within 20 minutes
		stopWatch.start();

		Attraction attraction = new Attraction("Disneyland", "Anaheim", null, 0, 0);
		allUsers.parallelStream().forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));
	     
	    allUsers.parallelStream().forEach(u -> rewardsService.calculateRewards(u));
	    
	    allUsers.parallelStream().forEach(user -> {
			assertTrue(user.getUserRewards().size() == 0);
		});
		stopWatch.stop();

		System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds."); 
		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}
	
}
