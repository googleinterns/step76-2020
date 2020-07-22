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

import com.google.common.primitives.Booleans;
import com.google.sps.data.Match;
import com.google.sps.data.MatchPreference;
import com.google.sps.data.Participant;
import com.google.sps.datastore.ParticipantDatastore;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/** Class used to find a match for new Participant with unmatched Participants in datastore */
public final class FindMatchQuery {

  /** Extra padding time in minutes to ensure large enough meeting time block */
  private static final int PADDING_MINUTES = 10;
  /** Minimum number of matching fields to be similar */
  private static final int MIN_SAME_FIELDS = 1;
  /** Reference clock */
  private final Clock clock;
  /** Datastore of Participants */
  private final ParticipantDatastore participantDatastore;

  /** Constructor */
  public FindMatchQuery(Clock clock, ParticipantDatastore participantDatastore) {
    this.clock = clock;
    this.participantDatastore = participantDatastore;
  }

  /**
   * @return Match of new participant with unmatched participants by comparing duration and
   *     availibility, or null if no match yet
   */
  @Nullable
  public Match findMatch(Participant newParticipant) {
    int duration = newParticipant.getDuration();

    // Get list of unmatched participants with same duration as newParticipant
    List<Participant> sameDurationParticipants =
        participantDatastore.getParticipantsWithDuration(duration);

    long firstEndTimeAvailable = newParticipant.getEndTimeAvailable();
    MatchPreference firstMatchPreference = newParticipant.getMatchPreference();
    String firstRole = newParticipant.getRole();
    String firstProductArea = newParticipant.getProductArea();

    // Compare new participant preferences with other participants to find match
    for (Participant currParticipant : sameDurationParticipants) {
      // Check if participants are both free for that duration + extra
      long secondEndTimeAvailable = currParticipant.getEndTimeAvailable();
      if (!isCompatibleTime(duration, firstEndTimeAvailable, secondEndTimeAvailable)) {
        System.out.println("not compatible time");
        continue;
      }

      // Check match preference compatibility and get combined preference if compatible
      MatchPreference secondMatchPreference = currParticipant.getMatchPreference();
      MatchPreference combinedMatchPreference =
          getCombinedMatchPreference(firstMatchPreference, secondMatchPreference);
      if (combinedMatchPreference == null) {
        System.out.println("not compatible match pref");
        continue;
      }

      // Check if combined match preference is satisfied depending on number of same fields
      if (!isCombinedMatchPreferenceSatisfied(
          combinedMatchPreference, newParticipant, currParticipant)) {
        continue;
      }

      // Found a match
      System.out.println("found a match");
      return new Match(
          newParticipant.getUsername(), currParticipant.getUsername(), duration, clock.millis());
    }
    // No inital match found
    return null;
  }

  /** Return true if compatible endTimeAvailable considering duration and padding */
  private boolean isCompatibleTime(
      int duration, long firstEndTimeAvailable, long secondEndTimeAvailable) {
    long earliestEndTimeAvailable = Math.min(firstEndTimeAvailable, secondEndTimeAvailable);
    return (clock.millis() + TimeUnit.MINUTES.toMillis(duration + PADDING_MINUTES))
        < earliestEndTimeAvailable;
  }

  /**
   * @return combined MatchPreference similar = both are similar OR one is similar and one is any;
   *     any = both are any; different = both are different = one is different and one is any. or
   *     null if match preferences are not compatible
   */
  private MatchPreference getCompatibleMatchPreference(
      MatchPreference firstMatchPreference, MatchPreference secondMatchPreference) {
    if ((firstMatchPreference == secondMatchPreference)
        || (firstMatchPreference == MatchPreference.ANY)
        || (secondMatchPreference == MatchPreference.ANY)) {
      return (firstMatchPreference == MatchPreference.ANY)
          ? secondMatchPreference
          : firstMatchPreference;
    } else {
      return null;
    }
  }

  /**
   * @return combined MatchPreference similar: both are similar, one is similar and one is any any:
   *     both are any different: both are different, one is different and one is any
   */
  private MatchPreference getCombinedMatchPreference(
      MatchPreference firstMatchPreference, MatchPreference secondMatchPreference) {
    return (firstMatchPreference == MatchPreference.ANY)
        ? secondMatchPreference
        : firstMatchPreference;
  }

  /**
   * @return true if the participants are a match based on their combinedMatchPreference and the
   *     number of fields that are the same, false if not a match
   */
  private boolean isCombinedMatchPreferenceSatisfied(
      MatchPreference combinedMatchPreference,
      Participant newParticipant,
      Participant currParticipant) {
    if (combinedMatchPreference != MatchPreference.ANY) {
      // Count number of same fields between the two participants
      boolean sameRole = newParticipant.getRole().equals(currParticipant.getRole());
      boolean sameProductArea =
          newParticipant.getProductArea().equals(currParticipant.getProductArea());
      int numSameFields = Booleans.countTrue(sameRole, sameProductArea);
      System.out.println("number of same fields: " + numSameFields);

      // If match preference = similar, numSameFields must be at least MIN_SAME_FIELDS to satisfy
      // preference
      // If match preference = differe, numSameFields must be less than MIN_SAME_FIELDS to satisfy
      // preference
      if ((combinedMatchPreference == MatchPreference.SIMILAR && numSameFields < MIN_SAME_FIELDS)
          || (combinedMatchPreference == MatchPreference.DIFFERENT
              && numSameFields >= MIN_SAME_FIELDS)) {
        return false;
      }
    }
    return true;
  }
}
