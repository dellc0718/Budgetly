package budgetlyDB.model;

import java.sql.Timestamp;

/**
 * Domain model representing a user's personal profile stored in USER_PROFILES.
 */
public class UserProfile {
    private int profileId;
    private int userId;
    private String profilePicture;
    private String displayName;
    private String contactNumber;
    private String address;
    private String bio;
    private String occupation;
    private Timestamp updatedAt;

    public UserProfile() {
        this.profilePicture = "default_croc";
    }

    public UserProfile(int profileId, int userId, String profilePicture, String displayName,
                       String contactNumber, String address, String bio, String occupation,
                       Timestamp updatedAt) {
        this.profileId = profileId;
        this.userId = userId;
        this.profilePicture = profilePicture;
        this.displayName = displayName;
        this.contactNumber = contactNumber;
        this.address = address;
        this.bio = bio;
        this.occupation = occupation;
        this.updatedAt = updatedAt;
    }

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
