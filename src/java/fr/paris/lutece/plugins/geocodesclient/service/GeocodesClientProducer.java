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
package fr.paris.lutece.plugins.geocodesclient.service;

import fr.paris.lutece.plugins.geocode.v1.web.rs.service.GeoCodeTransportRest;
import fr.paris.lutece.plugins.geocode.v1.web.rs.service.HttpAccessTransport;
import fr.paris.lutece.plugins.geocode.v1.web.service.GeoCodeService;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * CDI producer for the GeoCodeService. Replaces the former Spring context that
 * wired the HttpAccessTransport, the GeoCodeTransportRest and the GeoCodeService.
 */
@ApplicationScoped
public class GeocodesClientProducer
{
    /**
     * Builds the GeoCodeService over a REST transport pointing to the configured API endpoint.
     *
     * @param strApiEndPointUrl
     *            the geocodes API endpoint URL, injected from the plugin properties
     * @return the configured GeoCodeService
     */
    @Produces
    @ApplicationScoped
    @Named( "geocodes.geoCodesService" )
    public GeoCodeService createGeoCodeService(
            @ConfigProperty( name = "geocodes.identitystore.ApiEndPointUrl" ) Optional<String> strApiEndPointUrl )
    {
        HttpAccessTransport transport = new HttpAccessTransport( );
        transport.setApiEndPointUrl( strApiEndPointUrl.orElse( "" ) );

        GeoCodeTransportRest transportRest = new GeoCodeTransportRest( transport );

        return new GeoCodeService( transportRest );
    }
}
