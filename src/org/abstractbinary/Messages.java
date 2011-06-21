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
        String body;
        String subject;
        String sender;
        String recipient;
        List<Book> apples;
        List<Book> oranges;

        private Message() {
        }

        public Message(JSONObject json) throws JSONException {
            this.identifier = json.getString("identifier");
            this.body = json.getString("body");
            this.subject = json.getString("subject");
            this.sender = json.getString("sender");
            this.recipient = json.getString("recipient");
            if (json.has("apples")) {
                this.apples = new ArrayList<Book>();
                this.oranges = new ArrayList<Book>();
            }
        }
    }

    /* Public members */
    Set<String> unread = new HashSet<String>();
    List<String> all = new ArrayList<String>();
    Map<String, List<Message>> messages =
        new HashMap<String, List<Message>>();
    String jsonString = "";

    /* Public API */

    private Messages() {
    }

    public Messages(JSONObject json) throws JSONException {
        JSONArray convList = json.getJSONArray("conversation_list");
        for (int i = 0; i < convList.length(); ++i)
            all.add(convList.getString(i));

        JSONArray unreadList = json.getJSONArray("unread");
        for (int i = 0; i < unreadList.length(); ++i) {
            unread.add(unreadList.getString(i));
        }

        JSONObject conversations = json.getJSONObject("conversations");
        for (String c : all) {
            List<Message> ms = new ArrayList<Message>();
            JSONArray msgs = conversations.getJSONArray(c);
            for (int i = 0; i < msgs.length(); ++i)
                ms.add(new Message(msgs.getJSONObject(i)));
            messages.put(c, ms);
        }

        jsonString = json.toString();
    }
}
