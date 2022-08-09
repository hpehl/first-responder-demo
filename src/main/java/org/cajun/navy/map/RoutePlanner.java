package org.cajun.navy.map;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;
import org.cajun.navy.model.mission.Location;
import org.cajun.navy.model.mission.MissionStep;
import retrofit2.Response;

import javax.ejb.ActivationConfigProperty;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.logging.Logger;

public class RoutePlanner {

    private static Logger logger = Logger.getLogger(RoutePlanner.class.getName());

    // TODO: Get config property
    private static String MAPBOX_ACCESS_TOKEN;

    public static List<MissionStep> getDirections(Location origin, Location destination, Location waypoint) {

        try {
            List<MissionStep> missionSteps = new ArrayList<>();
            Response<DirectionsResponse> response = callMapBoxAPI(DirectionsCriteria.PROFILE_DRIVING, origin, destination, waypoint);

            if (response.body() == null || response.body().routes().isEmpty()) {
                logger.warning("No routes found. Origin: " + origin + "; Destination: " + destination + "; Waypoint: " + waypoint + ". Trying with profile cycling");
                response = callMapBoxAPI(DirectionsCriteria.PROFILE_CYCLING, origin, destination, waypoint);
                if (response.body() == null || response.body().routes().isEmpty()) {
                    logger.warning("No routes found with profile driving or cycling. Returning minimal mission steps array");
                    missionSteps.add(MissionStep.builder(origin.getLatitude().setScale(4, RoundingMode.HALF_UP),
                            origin.getLongitude().setScale(4, RoundingMode.HALF_UP)).build());
                    missionSteps.add(MissionStep.builder(waypoint.getLatitude().setScale(4, RoundingMode.HALF_UP),
                            waypoint.getLongitude().setScale(4, RoundingMode.HALF_UP)).wayPoint(true).build());
                    missionSteps.add(MissionStep.builder(destination.getLatitude().setScale(4, RoundingMode.HALF_UP),
                            destination.getLongitude().setScale(4, RoundingMode.HALF_UP)).destination(true).build());
                    return missionSteps;
                }
            }

            Optional<List<RouteLeg>> legs = Optional.ofNullable(response.body().routes().get(0).legs());
            legs.orElse(Collections.emptyList()).stream().flatMap(r -> Optional.ofNullable(r.steps()).orElse(Collections.emptyList()).stream())
                    .map(l -> {
                        Point p = l.maneuver().location();
                        MissionStep.Builder builder = MissionStep.builder(BigDecimal.valueOf(p.latitude()).setScale(4, RoundingMode.HALF_UP),
                                BigDecimal.valueOf(p.longitude()).setScale(4, RoundingMode.HALF_UP));
                        if ("arrive".equalsIgnoreCase(l.maneuver().type())) {
                            Optional<MissionStep> step = missionSteps.stream().filter(MissionStep::isWayPoint).findFirst();
                            if (step.isEmpty()) {
                                builder.wayPoint(true);
                            } else {
                                builder.destination(true);
                            }
                        }
                        return builder.build();
                    }).forEach(missionSteps::add);
            return missionSteps;
        } catch (IOException e) {
            logger.severe("Exception while calling MapBox API"+e.getMessage());
            throw new RoutePlannerException(e.getMessage(), e);
        }
    }

    private static Response<DirectionsResponse> callMapBoxAPI(String profile, Location origin, Location destination, Location waypoint) throws IOException {
        MapboxDirections request =  MapboxDirections.builder()
                .accessToken(MAPBOX_ACCESS_TOKEN)
                .origin(Point.fromLngLat(origin.getLongitude().doubleValue(), origin.getLatitude().doubleValue()))
                .destination(Point.fromLngLat(destination.getLongitude().doubleValue(), destination.getLatitude().doubleValue()))
                .addWaypoint(Point.fromLngLat(waypoint.getLongitude().doubleValue(), waypoint.getLatitude().doubleValue()))
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .profile(profile)
                .steps(true)
                .build();

        Response<DirectionsResponse> response = request.executeCall();

        // Check for error from MapBoxAPI
        if (!response.isSuccessful()) {
            logger.warning("Error when calling MapBoxAPI. Error message: " + response.message());
            throw new RoutePlannerException("MapBoxAPI error: " + response.message());
        }

        return response;
    }

}