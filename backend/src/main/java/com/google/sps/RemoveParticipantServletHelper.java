// Copyright 2020 Google LLC
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

package com.google.sps;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.sps.datastore.ParticipantDatastore;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Helper class to execute doPost for RemoveParticipantServlet */
public final class RemoveParticipantServletHelper {

  // HttpServlet request and response
  private final HttpServletRequest request;
  private final HttpServletResponse response;

  // ParticipantDatastore
  private final ParticipantDatastore participantDatastore;

  public RemoveParticipantServletHelper(
      HttpServletRequest request,
      HttpServletResponse response,
      ParticipantDatastore participantDatastore) {
    this.request = request;
    this.response = response;
    this.participantDatastore = participantDatastore;
  }

  public void doPostHelper() throws IOException {
    // Get username of participant sending exit request
    String username = getUsername();
    if (username == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not retrieve email.");
      return;
    }

    // Remove participant from datastore by username
    participantDatastore.removeParticipant(username);

    // Confirm participant exit queue request
    response.setContentType("text/plain;charset=UTF-8");
    response.getWriter().println("Received remove request.");
  }

  /** Retrieve user email address via Users API and parse for username */
  public String getUsername() {
    UserService userService = UserServiceFactory.getUserService();
    String email = userService.getCurrentUser().getEmail();
    return email != null ? email.split("@")[0] : null;
  }
}