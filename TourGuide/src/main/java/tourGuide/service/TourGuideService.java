package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.dto.NearbyAttractionDTO;
import tourGuide.helper.InternalTestHelper;
import tourGuide.user.User;
import tourGuide.user.UserReward;
import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardCentral;
	private final RewardsService rewardsService;
	private final ExecutorService executorService;
	private final TripPricer tripPricer = new TripPricer();
	boolean testMode = true;

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService, 
			RewardCentral rewardCentral, ExecutorService executorService) {
		this.gpsUtil = gpsUtil;
		this.rewardCentral = rewardCentral;
		this.rewardsService = rewardsService;
		this.executorService = executorService;

		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ? 
				user.getLastVisitedLocation() : gpsUtil.getUserLocation(user.getUserId());
		user.addToVisitedLocations(visitedLocation);
		 return visitedLocation;
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return new ArrayList<>(internalUserMap.values());
	}

	public Map<UUID, Location> getAllUsersDTO() {
		Map<UUID, Location> locationByUserIdMap = new HashMap <>();
		for (User user : getAllUsers()) {
			locationByUserIdMap.put(user.getUserId(), user.getLastVisitedLocation().location);
		}
		return locationByUserIdMap;
	}

	public void addUser(User user) {
		if(!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public List<Provider> getTripDeals(User user) throws InterruptedException, ExecutionException {
		int cumulatativeRewardPoints = user.getUserRewards().parallelStream()
				.mapToInt(i -> i.getRewardPoints()).sum();
		Future<List<Provider>> f = executorService.submit(() -> {
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, 
				user.getUserId(), 
				user.getUserPreferences().getNumberOfAdults(), 
				user.getUserPreferences().getNumberOfChildren(), 
				user.getUserPreferences().getTripDuration(), 
				cumulatativeRewardPoints);
		
		user.setTripDeals(providers);
		return providers;
		});
		return f.get();
	}

    public CompletableFuture<?> trackUserLocation(User user) {
        return CompletableFuture.supplyAsync(() -> gpsUtil.getUserLocation(user.getUserId()))
                .thenAccept(user::addToVisitedLocations)
                .thenRunAsync(() -> rewardsService.calculateRewards(user));
    }

	public List<NearbyAttractionDTO> getNearbyAttractions(String userName) {
		Location userLocation = getUserLocation(getUser(userName)).location;

		return gpsUtil.getAttractions().stream()
				.map(attraction -> {
					NearbyAttractionDTO newAttraction = new NearbyAttractionDTO();
					Location attractionLocation = new Location(attraction.longitude, attraction.latitude);
		
					newAttraction.setUserLocation(userLocation);
					newAttraction.setNameAttraction(attraction.attractionName);
					newAttraction.setAttractionLocation(attractionLocation);
					newAttraction.setDistance((int) rewardsService.getDistance(
							userLocation, attractionLocation));
					newAttraction.setRewardPoints(rewardCentral.getAttractionRewardPoints(
							getUser(userName).getUserId(), attraction.attractionId));
					
					return newAttraction; 
				})
				.sorted(Comparator.comparing(NearbyAttractionDTO::getDistance))
				.limit(5)
				.collect(Collectors.toList());
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();
	private void initializeInternalUsers() {
		//executorService.execute(() -> {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).parallel().forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);
//System.out.println("COUCOU");
			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
		//});
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i-> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(
					generateRandomLatitude(), generateRandomLongitude()
			), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}
