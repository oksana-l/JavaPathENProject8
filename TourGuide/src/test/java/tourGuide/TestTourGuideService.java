package tourGuide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.dto.NearbyAttractionDTO;
import tourGuide.helper.InternalTestHelper;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;
import tripPricer.Provider;

public class TestTourGuideService {

	private GpsUtil gpsUtil;
	private RewardsService rewardsService;
	private RewardCentral rewardCentral;
	private TourGuideService tourGuideService;
	private User user;
	private UUID uuid;
	private VisitedLocation visitedLocation;
	
	@Before
	public void setUp() {
		gpsUtil = mock(GpsUtil.class);
		rewardsService = mock(RewardsService.class);
		rewardCentral = mock(RewardCentral.class);
		tourGuideService = new TourGuideService(gpsUtil, rewardsService, rewardCentral, null);
		InternalTestHelper.setInternalUserNumber(0);
		uuid = UUID.randomUUID();
		user = new User(uuid, "jon", "000", "jon@tourGuide.com");
		Location location = new Location(100, 200);
		visitedLocation = new VisitedLocation(uuid, location, new Date());
	}
	@Ignore
	@Test
	public void shouldGetUserLocationTest() throws InterruptedException, ExecutionException {
		when(gpsUtil.getUserLocation(user.getUserId())).thenReturn(visitedLocation);
		VisitedLocation testVisitedLocation = tourGuideService.getUserLocation(user);
		
		assertTrue(testVisitedLocation.userId.equals(user.getUserId()));
	}
	
	@Test
	public void shouldAddUserTest() {
		User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

		tourGuideService.addUser(user);
		tourGuideService.addUser(user2);
		
		User retrivedUser = tourGuideService.getUser(user.getUserName());
		User retrivedUser2 = tourGuideService.getUser(user2.getUserName());

		assertEquals(user, retrivedUser);
		assertEquals(user2, retrivedUser2);
	}
	
	@Test
	public void shouldGetAllUsersTest() {
		User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

		tourGuideService.addUser(user);
		tourGuideService.addUser(user2);
		
		List<User> allUsers = tourGuideService.getAllUsers();

		assertTrue(allUsers.contains(user));
		assertTrue(allUsers.contains(user2));
	}
	@Ignore
	@Test
	public void shouldTrackUserTest() throws InterruptedException, ExecutionException {
		when(gpsUtil.getUserLocation(any())).thenReturn(visitedLocation);
		doNothing().when(rewardsService).calculateRewards(any());
		rewardsService.calculateRewards(user);
		VisitedLocation testVisitedLocation = (VisitedLocation) tourGuideService.trackUserLocation(user).join();

		assertEquals(user.getUserId(), testVisitedLocation.userId);
	}
	
	@Test
	public void shouldGetNearbyAttractionsTest() throws InterruptedException, ExecutionException {
		List<Attraction> attractionsList = new ArrayList<Attraction>() {
			private static final long serialVersionUID = 1L;
			{
			add(new Attraction("Disneyland", "Anaheim", null, 0, 0));
			add(new Attraction("Legend Valley", "Thornville", null, 0, 0));
			add(new Attraction("McKinley Tower", "Anchorage", null, 0, 0));
			add(new Attraction("Flatiron Building", "New York City", null, 0, 0));
			add(new Attraction("Fallingwater", "Mill Run", null, 0, 0));
			add(new Attraction("Union Station", "Washington D.C.", null, 0, 0));
			}
		};
		List<VisitedLocation> visitedLocationsList = new ArrayList<VisitedLocation>();
		visitedLocationsList.add(visitedLocation);
		user.setVisitedLocations(visitedLocationsList);
		tourGuideService.addUser(user);
		
		when(gpsUtil.getUserLocation(user.getUserId())).thenReturn(visitedLocation);
		when(gpsUtil.getAttractions()).thenReturn(attractionsList);
		when(rewardCentral.getAttractionRewardPoints(any(), any())).thenReturn(10);
		
		
		List<NearbyAttractionDTO> attractionsDto = tourGuideService.getNearbyAttractions("jon");
		assertEquals(5, attractionsDto.size());
	}
	
	public void shouldGetTripDealsTest() throws InterruptedException, ExecutionException {
		List<Provider> providers = tourGuideService.getTripDeals(user);
		
		assertEquals(10, providers.size());
	}
	
	
}
