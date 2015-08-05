package openbisconnector;

/*
 * Copyright 2015 ETH Zuerich, CISD
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import ch.systemsx.cisd.openbis.dss.client.api.v1.IOpenbisServiceFacade;
import ch.systemsx.cisd.openbis.dss.client.api.v1.impl.OpenbisServiceFacade;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.NewDataSetDTO;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.NewDataSetDTO.DataSetOwnerType;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.NewDataSetDTOBuilder;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.NewDataSetMetadataDTO;

/**
 * @author pkupczyk
 */
public class SSDM2055
{

    public static void main(String[] args)
    {
        File file = new File("src/test.txt");

        NewDataSetDTOBuilder builder = new NewDataSetDTOBuilder();
        builder.setDataSetOwnerType(DataSetOwnerType.EXPERIMENT);
        builder.setDataSetOwnerIdentifier("/TMARKER_DEMO/TMARKER_DEMO/TMARKER_DEMO");
        builder.setFile(file);

        NewDataSetMetadataDTO dataSetMetadata = builder.getDataSetMetadata();
        dataSetMetadata.setDataSetTypeOrNull("TMARKER");

        Map<String, String> map = new HashMap<String, String>();
        map.put("TMARKER_ID", "1");
        map.put("TMARKER_FILE_NAME", "Test filen ame");
        map.put("TMARKER_PATIENT", "Test patient");
        map.put("TMARKER_DESCRIPTION", "Test description");
        dataSetMetadata.setProperties(map);

        NewDataSetDTO dataSetDTO = builder.asNewDataSetDTO();

        IOpenbisServiceFacade facade = null;
        try
        {
            facade = getService();
            facade.putDataSet(dataSetDTO, file);
        } finally
        {
            if (facade != null)
            {
                facade.logout();
            }
        }
    }

    private static IOpenbisServiceFacade getService()
    {
        return OpenbisServiceFacade.tryCreate("user", "pw", "https://nexus-openbis.ethz.ch", 100000);
    }

}