import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

class UserInfo {
    private String handle;
    private String email;
    private String vkId;
    private String openId;
    private String firstName;
    private String lastName;
    private String country;
    private String city;
    private String organization;
    private String rank;
    private String maxRank;
    private String avatar;
    private String titlePhoto;
    private int contribution;
    private int rating;
    private int maxRating;
    private long lastOnlineTimeSeconds;
    private long registrationTimeSeconds;
    private int friendOfCount;

    private UserInfo() {
    }

    static UserInfo anonymous() {
        UserInfo ret = new UserInfo();
        ret.handle = "Anonymous";
        return ret;
    }

    static UserInfo fromServer(String handle) throws IOException {
        String text = WebUtils.getString("http://codeforces.com/api/user.info?handles=" + handle);
        JsonParser parser = new JsonParser();
        JsonObject object = parser.parse(text).getAsJsonObject()
                .get("result").getAsJsonArray()
                .get(0).getAsJsonObject();
        return new Gson().fromJson(object, UserInfo.class);
    }
}
