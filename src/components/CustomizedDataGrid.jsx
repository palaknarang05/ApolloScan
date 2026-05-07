import * as React from 'react';
import { DataGrid } from '@mui/x-data-grid';
import { columns } from '../internals/data/gridData';
import { BACKEND_URL } from '../constants';
import axios from 'axios';
import { IndexContext } from "../IndexProvider"

export default function CustomizedDataGrid() {
  const [rows, setRowsData] = React.useState([])
  const [metricData, setMetricData] = React.useState([])

  React.useEffect(() => {
    axios
      .put(`${BACKEND_URL}/scan-documents-with-templates`, {
        path: "repositoryPath",
        token: "token",
      })
      .then((response) => {

         if (response.data.length > 0) {
           setFullResponse(response.data);
           let filtered = [];
           response.data.map((item) => {
             filtered.push(item);
           });
           setMetricData(filtered);
         } else {
          console.warn("empty response for all scanned repo");
         }
      });
  }, []);

  React.useEffect(() => {
    metricData.length > 0 && metricData.map((obj, index) => {
      let curr = {}
      obj["issues"].map(issue => {
        curr.put("id", obj["id"])
        curr.put("filename", obj["fileName"])
        curr.put("aiCompatibilityScore", obj["response"]["aiCompatibilityScore"])
        curr.put("cyclomaticComplexity", obj["response"]["cyclomaticComplexity"])
        curr.put("couplingLevel", obj["response"]["couplingLevel"])
        curr.put("dynamicCodeConstructs", obj["response"]["dynamicCodeConstructs"])
        curr.put("relevantComments", obj["response"]["relevantComments"])
        curr.put("issueId", issue["issueId"])
        curr.put("start", issue["start"])
        curr.put("end", issue["end"])
        curr.put("severity", issue["severity"])
        curr.put("confidence", issue["confidence"])
        curr.put("description", issue["description"])
        curr.put("suggestedFix", issue["suggestedFix"])
      })
     setRowsData([...rows, curr])
    })
  }, [metricData])

  return (
    <DataGrid
      checkboxSelection
      rows={rows}
      columns={columns}
      getRowClassName={(params) =>
        params.indexRelativeToCurrentPage % 2 === 0 ? 'even' : 'odd'
      }
      initialState={{
        pagination: { paginationModel: { pageSize: 20 } },
      }}
      pageSizeOptions={[10, 20, 50]}
      disableColumnResize
      density="compact"
      slotProps={{
        filterPanel: {
          filterFormProps: {
            logicOperatorInputProps: {
              variant: 'outlined',
              size: 'small',
            },
            columnInputProps: {
              variant: 'outlined',
              size: 'small',
              sx: { mt: 'auto' },
            },
            operatorInputProps: {
              variant: 'outlined',
              size: 'small',
              sx: { mt: 'auto' },
            },
            valueInputProps: {
              InputComponentProps: {
                variant: 'outlined',
                size: 'small',
              },
            },
          },
        },
      }}
    />
  );
}