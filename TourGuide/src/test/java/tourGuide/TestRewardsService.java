package tourGuide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.helper.InternalTestHelper;
import tourGuide.service.RewardsService;
import tourGuide.user.User;
import tourGuide.user.UserReward;

public class TestRewardsService {

	private TourGuideModule tourGuideModule;
	private GpsUtil gpsUtil;
	private RewardCentral rewardCentral;
	private RewardsService rewardsService;
	private ExecutorService executorService;
	
	@Before
	public void setUp() {
		tourGuideModule = new TourGuideModule();
		gpsUtil = tourGuideModule.getGpsUtil();
		rewardCentral = tourGuideModule.getRewardCentral();
		executorService = tourGuideModule.getExecutorService();
		rewardsService = new RewardsService(gpsUtil, rewardCentral, executorService);
	}
	
	@Ignore // Needs fixed
	@Test
	public void userGetRewards() {
		when(rewardCentral.getAttractionRewardPoints(any(), any())).thenReturn(255);
		InternalTestHelper.setInternalUserNumber(0);
		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		Attraction attraction = new Attraction("Disneyland", "Anaheim", null, 0, 0);
		user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));

		List<UserReward> userRewards = user.getUserRewards();
		
		assertEquals(userRewards.size(),1);
	}
	
	@Test
	public void isWithinAttractionProximity() {
		Attraction attraction = new Attraction("Disneyland", "Anaheim", null, 0, 0);
		assertTrue(rewardsService.isWithinAttractionProximity(attraction, attraction));
	}
	
	@Ignore // Needs fixed - can throw ConcurrentModificationException
	@Test
	public void nearAllAttractions() {
		rewardsService.setProximityBuffer(Integer.MAX_VALUE);

		InternalTestHelper.setInternalUserNumber(1);
		
		//assertEquals(gpsUtil.getAttractions().size(), userRewards.size());
	}
	
}
