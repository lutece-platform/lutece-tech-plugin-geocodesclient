/*
 * Copyright (c) 2002-2024, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.geocodesclient.rs;

import fr.paris.lutece.plugins.geocode.v1.web.rs.dto.City;
import fr.paris.lutece.plugins.geocode.v1.web.rs.dto.Country;
import fr.paris.lutece.plugins.geocode.v1.web.service.GeoCodeService;
import fr.paris.lutece.plugins.rest.service.RestConstants;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.date.DateUtil;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonResponse;
import fr.paris.lutece.util.json.JsonUtil;
import org.apache.commons.lang3.StringUtils;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * CityRest
 */
@RequestScoped
@Path( RestConstants.BASE_PATH + Constants.API_PATH + Constants.VERSION_PATH )
public class GeocodesRest
{
    private static final int VERSION_1 = 1;
    private static final int cityMinChars = AppPropertiesService.getPropertyInt( "geocodes.city.minchars", 3);
    private static final int countryMinChars = AppPropertiesService.getPropertyInt( "geocodes.country.minchars", 3);
    public static final String ERROR_DATE_RESOURCE = "The date additionalParam must be entered";

    @Inject
    private GeoCodeService _geoCodesService;

    /**
     * Builds a fresh date format for parsing the reference date.
     *
     * Created lazily per call (not in a static initializer) so the class can be loaded during
     * server startup before the Lutece core services are ready, and to stay thread-safe
     * ({@link DateFormat} is not). Honors the optional {@code geocodes.override.default.date.pattern}
     * property, otherwise falls back to the core French locale format.
     *
     * @return a new DateFormat instance
     */
    private static DateFormat getDefaultDateFormat( )
    {
        final String datePatternFromProperty = AppPropertiesService.getProperty( "geocodes.override.default.date.pattern" );
        if ( StringUtils.isNotBlank( datePatternFromProperty ) )
        {
            return new SimpleDateFormat( datePatternFromProperty );
        }
        return DateUtil.getDateFormat( Locale.FRANCE );
    }

    /**
     * Get City List with date
     * 
     * @param nVersion
     *            the API version
     * @param strVal
     *            the search string
     * @param dateRef
     *            the reference date
     * @return the City List
     */
    @GET
    @Path( Constants.CITY_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getCityListByDate( @PathParam( Constants.VERSION ) Integer nVersion, @QueryParam( Constants.SEARCHED_STRING ) String strVal,
            @QueryParam( Constants.ADDITIONAL_PARAM ) String strDateRef )
    {
        if ( nVersion == VERSION_1 )
        {
            if ( strDateRef == null || strDateRef.isEmpty( ) )
            {
            	AppLogService.error( ERROR_DATE_RESOURCE );
	            return Response.status( Response.Status.NOT_FOUND )
	                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( Response.Status.NOT_FOUND.name( ), ERROR_DATE_RESOURCE ) ) )
	                    .build( );
            }
        	final Date dateref;
            try
            {
                dateref = getDefaultDateFormat( ).parse( strDateRef );
            }
            catch( final ParseException e )
            {
                AppLogService.error( e.getMessage( ) );
                return Response.status( Response.Status.BAD_REQUEST ).entity( e.getMessage( ) ).build( );
            }
            return getCityListByNameAndDateV1( strVal, dateref );
        }
        AppLogService.error( Constants.ERROR_NOT_FOUND_VERSION );
        return Response.status( Response.Status.NOT_FOUND )
                .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( Response.Status.NOT_FOUND.name( ), Constants.ERROR_NOT_FOUND_VERSION ) ) ).build( );
    }

    /**
     * set display values
     * 
     * @param lstCities
     */
    private void fillCitiesDisplayValues( List<City> lstCities )
    {
        lstCities.stream( ).forEach( c -> c.setDisplayValue( c.getValue( ) + " (" + c.getCodeZone( ) + ")" ) );
    }

    /**
     * Get City List V1
     * 
     * @return the City List for the version 1
     */
    private Response getCityListByNameAndDateV1( String strSearchBeginningVal, Date dateCity )
    {
        List<City> lstCities = new ArrayList<>( );

        if ( strSearchBeginningVal != null || strSearchBeginningVal.length( ) >= cityMinChars )
        {
            try
            {
                lstCities = _geoCodesService.getListCitiesByNameAndDateLike( strSearchBeginningVal, dateCity );
                fillCitiesDisplayValues( lstCities );
            }
            catch( Exception e )
            {
                AppLogService.error( e );
            }
        }

        return Response.status( Response.Status.OK ).entity( JsonUtil.buildJsonResponse( new JsonResponse( lstCities ) ) ).build( );
    }

    /**
     * Get City with date
     * 
     * @param nVersion
     *            the API version
     * @param strCode
     *            the search string
     * @param dateRef
     *            the reference date
     * @return the City List
     */
    @GET
    @Path( Constants.CITY_PATH + Constants.CITY_CODE_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getCityByCodeAndDate( @PathParam( Constants.VERSION ) Integer nVersion, @QueryParam( Constants.SEARCHED_CODE ) String strCode,
            @QueryParam( Constants.ADDITIONAL_PARAM ) String strDateRef )
    {
        if ( nVersion == VERSION_1 )
        {
            final Date dateref;
            if ( strDateRef == null || strDateRef.isEmpty( ) )
            {
            	AppLogService.error( ERROR_DATE_RESOURCE );
	            return Response.status( Response.Status.NOT_FOUND )
	                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( Response.Status.NOT_FOUND.name( ), ERROR_DATE_RESOURCE ) ) )
	                    .build( );
            }
            try
            {
                dateref = getDefaultDateFormat( ).parse( strDateRef );
            }
            catch( final ParseException e )
            {
                AppLogService.error( e.getMessage( ) );
                return Response.status( Response.Status.BAD_REQUEST ).entity( e.getMessage( ) ).build( );
            }
            return getCityByCodeAndDateV1( strCode, dateref );
        }
        AppLogService.error( Constants.ERROR_NOT_FOUND_VERSION );
        return Response.status( Response.Status.NOT_FOUND )
                .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( Response.Status.NOT_FOUND.name( ), Constants.ERROR_NOT_FOUND_VERSION ) ) ).build( );
    }

    /**
     * Get City by code and date V1
     * 
     * @return the City for the version 1
     */
    private Response getCityByCodeAndDateV1( String strCode, Date dateCity )
    {
        City city = new City( );
        try
        {
            city = _geoCodesService.getCityByCodeAndDate( strCode, dateCity );
        }
        catch( Exception e )
        {
            AppLogService.error( e );
        }

        return Response.status( Response.Status.OK ).entity( JsonUtil.buildJsonResponse( new JsonResponse( city ) ) ).build( );
    }

    /**
     * Get City with date
     * 
     * @param nVersion
     *            the API version
     * @param strCode
     *            the search string
     * @param dateRef
     *            the reference date
     * @return the City List
     */
    @GET
    @Path( Constants.COUNTRY_PATH + Constants.COUNTRY_CODE_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getCountryByCodeAndDate( @PathParam( Constants.VERSION ) Integer nVersion, @QueryParam( Constants.SEARCHED_CODE ) String strCode,
            @QueryParam( Constants.ADDITIONAL_PARAM ) String strDateRef )
    {
        if ( nVersion == VERSION_1 )
        {
        	if ( strDateRef == null || strDateRef.isEmpty( ) )
            {
            	AppLogService.error( ERROR_DATE_RESOURCE );
	            return Response.status( Response.Status.NOT_FOUND )
	                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( Response.Status.NOT_FOUND.name( ), ERROR_DATE_RESOURCE ) ) )
	                    .build( );
            }
        	final Date dateref;
            try
            {
                dateref = getDefaultDateFormat( ).parse( strDateRef );
            }
            catch( final ParseException e )
            {
                AppLogService.error( e.getMessage( ) );
                return Response.status( Response.Status.BAD_REQUEST ).entity( e.getMessage( ) ).build( );
            }
            return getCountryByCodeAndDateV1( strCode, dateref );
        }
        AppLogService.error( Constants.ERROR_NOT_FOUND_VERSION );
        return Response.status( Response.Status.NOT_FOUND )
                .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( Response.Status.NOT_FOUND.name( ), Constants.ERROR_NOT_FOUND_VERSION ) ) ).build( );
    }

    /**
     * Get City by code and date V1
     * 
     * @return the City for the version 1
     */
    private Response getCountryByCodeAndDateV1( String strCode, Date dateCity )
    {
        Country country = new Country( );
        try
        {
            country = _geoCodesService.getCountryByCodeAndDate( strCode, dateCity );
        }
        catch( Exception e )
        {
            AppLogService.error( e );
        }

        return Response.status( Response.Status.OK ).entity( JsonUtil.buildJsonResponse( new JsonResponse( country ) ) ).build( );
    }

    /**
     * Search Countries
     * 
     * @param nVersion
     *            the API version
     * @param search
     *            the searched string
     * @param dateRef
     *            the reference date
     * @return the Countries
     */
    @GET
    @Path( Constants.COUNTRY_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getCountiesListByNameAndDate( @PathParam( Constants.VERSION ) Integer nVersion, @QueryParam( Constants.SEARCHED_STRING ) String strVal,
            @QueryParam( Constants.ADDITIONAL_PARAM ) String strDateRef )
    {
        if ( nVersion == VERSION_1 )
        {
        	if ( strDateRef == null || strDateRef.isEmpty( ) )
            {
            	AppLogService.error( ERROR_DATE_RESOURCE );
	            return Response.status( Response.Status.NOT_FOUND )
	                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( Response.Status.NOT_FOUND.name( ), ERROR_DATE_RESOURCE ) ) )
	                    .build( );
            }
        	final Date dateref;
            try
            {
                dateref = getDefaultDateFormat( ).parse( strDateRef );
            }
            catch( final ParseException e )
            {
                AppLogService.error( e.getMessage( ) );
                return Response.status( Response.Status.BAD_REQUEST ).entity( e.getMessage( ) ).build( );
            }
            return getCountriesListByNameAndDateV1( strVal, dateref );
        }

        AppLogService.error( Constants.ERROR_NOT_FOUND_VERSION );
        return Response.status( Response.Status.NOT_FOUND )
                .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( Response.Status.NOT_FOUND.name( ), Constants.ERROR_NOT_FOUND_VERSION ) ) ).build( );
    }

    /**
     * Get Country V1
     * 
     * @param id
     *            the id
     * @return the Country for the version 1
     */
    private Response getCountriesListByNameAndDateV1( String strSearchBeginningVal, Date dateRef )
    {
        List<Country> listCountries = new ArrayList<>( );

        if ( strSearchBeginningVal != null && strSearchBeginningVal.length( ) >= countryMinChars )
        {
            try
            {
                listCountries = _geoCodesService.getListCountryByNameAndDate( strSearchBeginningVal, dateRef );
            }
            catch( Exception e )
            {
                AppLogService.error( e );
            }
        }

        return Response.status( Response.Status.OK ).entity( JsonUtil.buildJsonResponse( new JsonResponse( listCountries ) ) ).build( );
    }
}
