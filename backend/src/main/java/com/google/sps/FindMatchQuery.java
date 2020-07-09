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

import com.google.sps.data.Match;
import com.google.sps.data.Participant;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;

/** Class used to find a match in a list of Participants with the most recently added Participant */
public final class FindMatchQuery {

  /** Maximum difference in duration to be compatible */
  private static int MAX_DURATION_DIFF = 15;
  /** extra padding time to ensure large enough meeting time block */
  private static int PADDING_TIME = 15;
  /** Reference clock */
  private Clock clock;

  /** Constructor */
  public FindMatchQuery(Clock clock) {
    this.clock = clock;
  }

  /**
   * Find match of new participant or add to list of participants, return whether or not match was
   * found right after being added
   */
  public Match findMatch(List<Participant> participants, Participant newParticipant) {
    // Set reference date time using clock
    ZonedDateTime dateTime = ZonedDateTime.now(clock);
    System.out.println(dateTime);

    // Compare new participant preferences with others in list to find match
    for (Participant currParticipant : participants) {

      // Check if participants are looking for similar meeting duration
      int newDuration = newParticipant.getDuration();
      int currDuration = currParticipant.getDuration();
      boolean compatibleDuration = Math.abs(newDuration - currDuration) <= MAX_DURATION_DIFF;

      if (!compatibleDuration) {
        continue;
      }
      int duration = Math.min(newDuration, currDuration);

      // Check if participants are both free for that duration + extra
      ZonedDateTime newEndTimeAvailable = newParticipant.getEndTimeAvailable();
      ZonedDateTime currEndTimeAvailable = currParticipant.getEndTimeAvailable();
      ZonedDateTime earliestEndTimeAvailable =
          getEarlier(newEndTimeAvailable, currEndTimeAvailable);
      boolean compatibleTime =
          dateTime.plusMinutes(duration + PADDING_TIME).isBefore(earliestEndTimeAvailable);

      if (compatibleTime) {
        // TODO: change match ID (currently -1 for easy error checking)
        return new Match(
            /* id= */ -1L,
            newParticipant,
            currParticipant,
            duration,
            dateTime.toInstant().toEpochMilli());
      }
    }
    // No inital match found
    return null;
  }

  /** Return earlier of two ZonedDateTime objects */
  private ZonedDateTime getEarlier(ZonedDateTime first, ZonedDateTime second) {
    return first.isBefore(second) ? first : second;
  }
}