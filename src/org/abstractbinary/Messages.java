package org.abstractbinary.booktrader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


class Messages {
    static class Message {
        String identifier;

        private Message() {
        }

        public Message(String identifier) {
            this.identifier = identifier;
        }
    }

    /* Public members */
    Set<String> unread = new HashSet<String>();
    List<String> all = new ArrayList<String>();
    Map<String, Message> messages = new HashMap<String, Message>();
    String jsonString = "";

    /* Public API */

    private Messages() {
    }

    public Messages(JSONObject json) throws JSONException {
        JSONArray convList = json.getJSONArray("conversation_list");
        for (int i = 0; i < convList.length(); ++i) {
            Message m = new Message(convList.getString(i));
            all.add(m.identifier);
            messages.put(m.identifier, m);
        }

        JSONArray unreadList = json.getJSONArray("unread");
        for (int i = 0; i < unreadList.length(); ++i) {
            unread.add(unreadList.getString(i));
        }

        jsonString = json.toString();
    }
}
