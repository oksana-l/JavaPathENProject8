package tourGuide.service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.user.User;
import tourGuide.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 5;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 2000000000;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	private final ExecutorService executorService;
	
	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral, 
				ExecutorService executorService) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
		this.executorService = executorService;
	}
	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}
	
	public void calculateRewards(User user) {
		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsUtil.getAttractions();
		
		executorService.submit(() -> {
			userLocations.parallelStream().forEach(visitedLocation ->  {
				attractions.parallelStream().forEach(attraction -> {
					if(user.getUserRewards().stream().filter(
							r -> r.attraction.attractionName.equals(
									attraction.attractionName))
							.count() == 0) {
						if(nearAttraction(visitedLocation, attraction)) {
							try {
								user.addUserReward(new UserReward(
										visitedLocation, attraction, getRewardPoints(attraction, user)
										.get()));
							} catch (Exception e) {
								// nothing TO DO
							}
						}
					}
				});
			});
		});
	}
	
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}
	
	private Future<Integer> getRewardPoints(Attraction attraction, User user)  {
		return executorService.submit(() -> rewardsCentral.getAttractionRewardPoints(
				attraction.attractionId, user.getUserId()));
	}
	
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}

}
