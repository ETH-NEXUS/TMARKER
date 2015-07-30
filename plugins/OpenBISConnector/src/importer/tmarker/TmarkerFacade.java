/*
 * Copyright 2015 ETH Zurich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package importer.tmarker;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet;
import ch.systemsx.cisd.openbis.dss.client.api.v1.IOpenbisServiceFacade;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.SearchCriteria;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.SearchCriteria.MatchClause;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.SearchCriteria.MatchClauseAttribute;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.SearchSubCriteria;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.SpaceWithProjectsAndRoleAssignments;
import common.config.Config;
import importer.common.facade.AbstractFacade;
import java.util.ArrayList;
import java.util.Collection;
import javax.imageio.ImageIO;
import openbisconnector.openBISConnector;

/**
 * @author Peter J. Schueffler
 */
public class TmarkerFacade extends AbstractFacade {
    
    openBISConnector obc = null;
    String space = "";

    public TmarkerFacade(Config config, openBISConnector obc, String space) {
        super(config);
        this.obc = obc;
        this.space = space;
    }

    @Override
    public List<DataSet> listDataSets() {
        
        /*System.out.println("Found Spaces For Datasets:");
        for (SpaceWithProjectsAndRoleAssignments space : service.getSpacesWithProjects()) {
            System.out.println("  " + space.getCode());
        }
        System.out.println();*/
        
        SearchCriteria searchCriteria = new SearchCriteria();
        if (space != null && !space.isEmpty()) {
            searchCriteria.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.SPACE, space));
        
            SearchCriteria dataSetCriteria = new SearchCriteria();
            dataSetCriteria.addSubCriteria(SearchSubCriteria.createExperimentCriteria(searchCriteria));

            return service.searchForDataSets(dataSetCriteria);
        }
        return service.searchForDataSets(searchCriteria);
        
    }

    @Override
    public List<Sample> listSamples() {
        
        /*System.out.println("Found Spaces For Samples:");
        for (SpaceWithProjectsAndRoleAssignments space : service.getSpacesWithProjects()) {
            System.out.println("  " + space.getCode());
        }
        System.out.println();*/
            
        SearchCriteria sampleCriteria = new SearchCriteria();
        if (space != null && !space.isEmpty()) {
            sampleCriteria.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.SPACE, space));
        } else {
            for (SpaceWithProjectsAndRoleAssignments space : service.getSpacesWithProjects()) {
                sampleCriteria.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.SPACE, space.getCode()));
            }
        }
        
        /*System.out.println("Found SampleTypes:");
         for (SampleType st: service.listSampleTypes()) {
         sampleCriteria.addMatchClause(MatchClause.createAttributeMatch(
         MatchClauseAttribute.TYPE, st.getCode()));
         System.out.println("  " + st.getCode());
         }
         System.out.println();*/
        /*
         sampleCriteria.setOperator(SearchOperator.MATCH_ANY_CLAUSES);
         SearchCriteria experimentCriteria = new SearchCriteria();
         System.out.println("Found ExperimentTypes:");
         for (ExperimentType et: service.listExperimentTypes()) {
         experimentCriteria.addMatchClause(MatchClause.createAttributeMatch(
         MatchClauseAttribute.TYPE, et.getCode()));
         System.out.println("  " + et.getCode());
         }
         System.out.println();
         sampleCriteria.addSubCriteria(SearchSubCriteria
         .createExperimentCriteria(experimentCriteria));*/

        return service.searchForSamples(sampleCriteria);
    }

    @Override
    public List<DataSet> listDataSetsBySample(String samplePermId) {
        return service.listDataSetsForSample(samplePermId);

        /*
         SearchCriteria sampleCriteria = new SearchCriteria();
         sampleCriteria.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.PERM_ID, samplePermId));

         SearchCriteria dataSetCriteria = new SearchCriteria();
         dataSetCriteria.setOperator(SearchOperator.MATCH_ALL_CLAUSES);
         System.out.println("DataSetTypes:");
         for (DataSetType dt: service.listDataSetTypes()) {
         dataSetCriteria.addMatchClause(MatchClause.createAttributeMatch(
         MatchClauseAttribute.TYPE, dt.getCode()));
         System.out.println("  " + dt.getCode());
         }
         System.out.println();
         //dataSetCriteria.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.TYPE, "MICROSCOPY_IMG_CONTAINER"));
         dataSetCriteria.addSubCriteria(SearchSubCriteria.createSampleCriteria(sampleCriteria));

         List<DataSet> containers = service.searchForDataSets(dataSetCriteria);
         return containers;
         /*if (containers.size() > 0)
         {
         DataSet container = containers.get(0);

         SearchCriteria containerCriteria = new SearchCriteria();
         containerCriteria.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.PERM_ID, container.getCode()));

         SearchCriteria containedCriteria = new SearchCriteria();
         containedCriteria.setOperator(SearchOperator.MATCH_ALL_CLAUSES);
         for (DataSetType dt: service.listDataSetTypes()) {
         containedCriteria.addMatchClause(MatchClause.createAttributeMatch(
         MatchClauseAttribute.TYPE, dt.getCode()));
         System.out.println(dt.getCode());
         }
         //containedCriteria.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.TYPE, "MICROSCOPY_IMG"));
         containedCriteria.addSubCriteria(SearchSubCriteria.createDataSetContainerCriteria(containerCriteria));

         return service.searchForDataSets(containedCriteria);
         } else
         {
         return Collections.emptyList();
         }*/
    }

    @Override
    public File downloadDataSet(String dataSetCode) {
        return downloadDataSet(dataSetCode, new FileFilter() {

            @Override
            public boolean accept(File file) {
                //return file.getName().endsWith(".lsm") || file.getName().endsWith(".nd2");
                Collection<String> allowed_ext = new ArrayList<>();
                allowed_ext.add("xml");
                allowed_ext.add("csv");
                allowed_ext.add("tif");
                allowed_ext.add("tiff");
                String[] its = ImageIO.getReaderFormatNames();
                for (int i=0; i<its.length; i++) {
                    allowed_ext.add(its[i].toLowerCase());
                }
                allowed_ext.add("ndpi");
                return allowed_ext.contains(file.getName().substring(file.getName().lastIndexOf(".") + 1));
            }

        });
    }

    //
    // Optional Methods
    //
    @Override
    public boolean isDownloadDataSetFileAvailable() {
        return false;
    }

    @Override
    public boolean isGetAllDataSetFilesAvailable() {
        return false;
    }

    /**
     * Returns the current openBIS service.
     *
     * @return The current openBIS service.
     */
    public IOpenbisServiceFacade getService() {
        return service;
    }
    
    /**
     * Returns the current openBISconnector.
     *
     * @return The current openBISconnector.
     */
    public openBISConnector getOBC() {
        return obc;
    }
    
    /**
     * Sets the space considered in this facade
     * @param space The space code of the considered Space.
     */
    public void setSpace(String space) {
        this.space = space;
    }
}