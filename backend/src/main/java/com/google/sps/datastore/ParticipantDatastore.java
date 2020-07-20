package com.google.sps.datastore;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.sps.data.MatchStatus;
import com.google.sps.data.Participant;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Separates datastore method calls involving Participant type from caller */
public final class ParticipantDatastore {

  // Datastore Key/Property constants
  private static final String KIND_PARTICIPANT = "Participant";
  private static final String PROPERTY_USERNAME = "username";
  private static final String PROPERTY_START_TIME_AVAILABLE = "startTimeAvailable";
  private static final String PROPERTY_END_TIME_AVAILABLE = "endTimeAvailable";
  private static final String PROPERTY_DURATION = "duration";
  private static final String PROPERTY_MATCH_ID = "matchId";
  private static final String PROPERTY_MATCH_STATUS = "matchStatus";
  private static final String PROPERTY_TIMESTAMP = "timestamp";

  /** Datastore */
  private final DatastoreService datastore;

  /** Constructor that takes in DatastoreService */
  public ParticipantDatastore(DatastoreService datastore) {
    this.datastore = datastore;
  }

  /** Return entity of participant */
  private static Entity getEntityFromParticipant(Participant participant) {
    // Set properties of entity based on participant fields
    Entity entity = new Entity(KIND_PARTICIPANT, participant.getUsername());
    entity.setProperty(PROPERTY_USERNAME, participant.getUsername());
    entity.setProperty(PROPERTY_START_TIME_AVAILABLE, participant.getStartTimeAvailable());
    entity.setProperty(PROPERTY_END_TIME_AVAILABLE, participant.getEndTimeAvailable());
    entity.setProperty(PROPERTY_DURATION, participant.getDuration());
    entity.setProperty(PROPERTY_MATCH_ID, participant.getMatchId());
    entity.setProperty(PROPERTY_MATCH_STATUS, participant.getMatchStatus().getValue());
    entity.setProperty(PROPERTY_TIMESTAMP, participant.getTimestamp());

    return entity;
  }

  /**
   * Put participant in datastore. Overwrite participant entity if participant with same username
   * already exists.
   */
  public void addParticipant(Participant participant) {
    // Insert entity into datastore
    datastore.put(getEntityFromParticipant(participant));
  }

  /** Return Participant Entity from username, or null if entity is not found */
  @Nullable
  private Entity getEntity(String username) {
    Key participantKey = KeyFactory.createKey(KIND_PARTICIPANT, username);
    try {
      return datastore.get(participantKey);
    } catch (EntityNotFoundException e) {
      return null;
    }
  }

  /** Return participant object from datastore participant entity, or null if entity is null */
  @Nullable
  private static Participant getParticipantFromEntity(@Nonnull Entity entity) {
    return new Participant(
        (String) entity.getProperty(PROPERTY_USERNAME),
        (long) entity.getProperty(PROPERTY_START_TIME_AVAILABLE),
        (long) entity.getProperty(PROPERTY_END_TIME_AVAILABLE),
        ((Long) entity.getProperty(PROPERTY_DURATION)).intValue(),
        (long) entity.getProperty(PROPERTY_MATCH_ID),
        MatchStatus.forIntValue(((Long) entity.getProperty(PROPERTY_MATCH_STATUS)).intValue()),
        (long) entity.getProperty(PROPERTY_TIMESTAMP));
  }

  /** Return Participant from username, or null if participant with username not in datastore */
  @Nullable
  public Participant getParticipantFromUsername(String username) {
    Entity entity = getEntity(username);
    if (entity == null) {
      return null;
    }
    return getParticipantFromEntity(entity);
  }

  /** Return list of all unmatched participants with duration */
  public List<Participant> getParticipantsWithDuration(int duration) {
    Query query = new Query(KIND_PARTICIPANT);

    // Create filter to get only unmatched participants with same duration
    Filter unmatched =
        new FilterPredicate(
            PROPERTY_MATCH_STATUS, FilterOperator.EQUAL, MatchStatus.UNMATCHED.getValue());
    Filter sameDuration = new FilterPredicate(PROPERTY_DURATION, FilterOperator.EQUAL, duration);
    query.setFilter(unmatched).setFilter(sameDuration);

    PreparedQuery results = datastore.prepare(query);

    // Convert entities to list of participants
    List<Participant> participants = new ArrayList<Participant>();
    for (Entity entity : results.asIterable()) {
      participants.add(getParticipantFromEntity(entity));
    }
    return participants;
  }

  /** Remove Participant from datastore */
  public void removeParticipant(String username) {
    Key participantKey = KeyFactory.createKey(KIND_PARTICIPANT, username);
    try {
      datastore.delete(participantKey);
    } catch (IllegalArgumentException e) {
      System.out.println(
          "Participant with username "
              + username
              + " cannot be removed because it is not in the datastore.");
    }
  }
}
