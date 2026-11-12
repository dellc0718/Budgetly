package budgetlyDB;

import budgetlyDB.model.UserProfile;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Data Access Object for USER_PROFILES.
 */
public class ProfileDAO {

    public UserProfile findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM USER_PROFILES WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public int insertProfile(Connection con, UserProfile profile) throws SQLException {
        String sql = "INSERT INTO USER_PROFILES "
                + "(user_id, profile_picture, display_name, contact_number, address, bio, occupation) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, profile.getUserId());
            ps.setString(2, safe(profile.getProfilePicture(), "default_croc"));
            ps.setString(3, safe(profile.getDisplayName(), "Budgetly User"));
            ps.setString(4, safe(profile.getContactNumber(), ""));
            ps.setString(5, safe(profile.getAddress(), ""));
            ps.setString(6, safe(profile.getBio(), ""));
            ps.setString(7, safe(profile.getOccupation(), ""));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    profile.setProfileId(id);
                    return id;
                }
                throw new SQLException("Inserting profile did not return a generated profile_id.");
            }
        }
    }

    public boolean upsertProfile(UserProfile profile) throws SQLException {
        UserProfile existing = findByUserId(profile.getUserId());
        if (existing == null) {
            try (Connection con = DatabaseConnection.getConnection()) {
                insertProfile(con, profile);
                return true;
            }
        }
        String sql = "UPDATE USER_PROFILES SET "
                + "profile_picture = ?, display_name = ?, contact_number = ?, "
                + "address = ?, bio = ?, occupation = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, safe(profile.getProfilePicture(), "default_croc"));
            ps.setString(2, safe(profile.getDisplayName(), "Budgetly User"));
            ps.setString(3, safe(profile.getContactNumber(), ""));
            ps.setString(4, safe(profile.getAddress(), ""));
            ps.setString(5, safe(profile.getBio(), ""));
            ps.setString(6, safe(profile.getOccupation(), ""));
            ps.setInt(7, profile.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    private UserProfile mapRow(ResultSet rs) throws SQLException {
        return new UserProfile(
                rs.getInt("profile_id"),
                rs.getInt("user_id"),
                rs.getString("profile_picture"),
                rs.getString("display_name"),
                rs.getString("contact_number"),
                rs.getString("address"),
                rs.getString("bio"),
                rs.getString("occupation"),
                rs.getTimestamp("updated_at")
        );
    }

    private static String safe(String value, String fallback) {
        return value == null ? fallback : value.trim();
    }
}
