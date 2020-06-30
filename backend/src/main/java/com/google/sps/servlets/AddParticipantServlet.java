// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.sps.FindMatchQuery;
import com.google.sps.data.Match;
import com.google.sps.data.Participant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns some example content. */
@WebServlet("/add-participant")
public class AddParticipantServlet extends HttpServlet {

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Request parameter values
    // TODO: Error check input values (before allowing to submit)
    UserService userService = UserServiceFactory.getUserService();
    String email = userService.getCurrentUser().getEmail();
    String ldap = email.split("@")[0];
    long endTimeAvailable = convertToPositiveLong(request.getParameter("endTimeAvailable"));
    String timezone = request.getParameter("timezone");
    int duration = convertToPositiveInt(request.getParameter("duration"));
    if (email == null || endTimeAvailable == -1L || duration == -1) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input(s).");
    }
    long timestamp = System.currentTimeMillis();

    // id is irrelevant, only relevant when getting from datastore
    Participant newParticipant = 
        new Participant(/* id= */ -1L, ldap, endTimeAvailable, timezone, duration, timestamp);
    
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    // Find immediate match if possible
    FindMatchQuery query = new FindMatchQuery();
    Match match = query.findMatch(getParticipants(datastore), newParticipant);

    // Match found, add to datastore, delete matched participants from datastore
    if (match != null) {
      addMatchToDatastore(match, datastore);
      deleteParticipantFromDatastore(match.getSecondParticipant(), datastore);
    } else {
      // Match not found, insert participant entity into datastore
      Entity participantEntity = new Entity("Participant");
      participantEntity.setProperty("ldap", ldap);
      participantEntity.setProperty("endTimeAvailable", endTimeAvailable);
      participantEntity.setProperty("timezone", timezone);
      participantEntity.setProperty("duration", duration);
      participantEntity.setProperty("timestamp", timestamp);
      datastore.put(participantEntity);
    }

    // Redirect back to the HTML page
    response.sendRedirect("/index.html");
  }

  /** Return list of current participants from datastore */
  private List<Participant> getParticipants(DatastoreService datastore) {

    // Create and sort participant queries by time
    Query query = new Query("Participant").addSort("timestamp", SortDirection.DESCENDING);
    PreparedQuery results = datastore.prepare(query);

    // Convert list of entities to list of participants
    List<Participant> participants = new ArrayList<Participant>();
    for (Entity entity : results.asIterable()) {
        long id = (long) entity.getKey().getId();
        String ldap = (String) entity.getProperty("ldap");
        long endTimeAvailable = (long) entity.getProperty("endTimeAvailable");
        String timezone = (String) entity.getProperty("timezone"); 
        int duration = (int) entity.getProperty("duration"); 
        long timestamp = (long) entity.getProperty("timestamp");
        Participant currParticipant = 
            new Participant(id, ldap, endTimeAvailable, timezone, duration, timestamp);
        participants.add(currParticipant);
    }
    return participants;
  }

  /** Add Match pair to datastore */
  private void addMatchToDatastore(Match match, DatastoreService datastore) {
    // Set properties of entity
    Entity matchEntity = new Entity("Match");
    matchEntity.setProperty("firstParticipant", match.getFirstParticipant());
    matchEntity.setProperty("secondParticipant", match.getSecondParticipant());
    matchEntity.setProperty("duration", match.getDuration());
    matchEntity.setProperty("timestamp", match.getTimestamp());

    // Insert entity into datastore
    datastore.put(matchEntity);
  }

  /** Delete matched participants from datastore */
  private void deleteParticipantFromDatastore(Participant participant, DatastoreService datastore) {
    Key participantEntityKey = KeyFactory.createKey("Participant", participant.getId());
    datastore.delete(participantEntityKey);
  }

  /** Return positive long value, or -1L if invalid or negative */
  private static long convertToPositiveLong(String s) {
    if (s == null) {
      return -1L;
    }
    try {
      long parsed = Long.parseLong(s);
      return (parsed >= 0L) ? parsed : -1L;
    } catch (NumberFormatException e) {
      return -1L;
    }
  }

  /** Return positive integer value, or -1 if invalid or negative */
  private static int convertToPositiveInt(String s) {
    if (s == null) {
      return -1;
    }
    try {
      int parsed = Integer.parseInt(s);
      return (parsed >= 0) ? parsed : -1;
    } catch (NumberFormatException e) {
      return -1;
    }
  }
}
