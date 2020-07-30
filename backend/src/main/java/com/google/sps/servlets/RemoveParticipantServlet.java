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

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.sps.RemoveParticipantServletHelper;
import com.google.sps.datastore.ParticipantDatastore;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that removes a participany from the queue */
@WebServlet("/api/v1/remove-participant")
public class RemoveParticipantServlet extends HttpServlet {

  private final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  private final ParticipantDatastore participantDatastore = new ParticipantDatastore(datastore);

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    RemoveParticipantServletHelper removeParticipantServletHelper =
        new RemoveParticipantServletHelper(request, response, participantDatastore);
    removeParticipantServletHelper.doPostHelper();
  }
}