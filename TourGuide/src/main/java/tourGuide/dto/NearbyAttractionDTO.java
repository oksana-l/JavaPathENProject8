package tourGuide.dto;

import gpsUtil.location.Location;

public class NearbyAttractionDTO implements Comparable<NearbyAttractionDTO> {

	private String nameAttraction;
	private Location userLocation;
	private Location attractionLocation;
	private int distance;
	private Integer rewardPoints;
	
	public NearbyAttractionDTO() {
		
	}

	public String getNameAttraction() {
		return nameAttraction;
	}

	public void setNameAttraction(String nameAttraction) {
		this.nameAttraction = nameAttraction;
	}

	public Location getUserLocation() {
		return userLocation;
	}

	public void setUserLocation(Location userLocation) {
		this.userLocation = userLocation;
	}

	public Location getAttractionLocation() {
		return attractionLocation;
	}

	public void setAttractionLocation(Location attractionLocation) {
		this.attractionLocation = attractionLocation;
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public Integer getRewardPoints() {
		return rewardPoints;
	}

	public void setRewardPoints(Integer rewardPoints) {
		this.rewardPoints = rewardPoints;
	}
	
	/*
	* Comparator pour le tri des attractions par distance 
	*/
	@Override
	public int compareTo(NearbyAttractionDTO d) {
		return this.distance - d.distance;
	}
}
