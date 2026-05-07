import * as React from 'react';
import {DataGrid} from '@mui/x-data-grid';
import Button from "@mui/material/Button";
import clsx from 'clsx';
import Chip from "@mui/material/Chip";

export default function PositivesDataGrid({handleOpen, report}) {
    const renderDetailsButton = (params) => {
        return (
            <Button
                variant="outlined"
                color="secondary"
                size="small"
                onClick={() => {
                    handleOpen(params.row["id"]);
                }}
            >
                More Info
            </Button>
        )
    }

    const customIcon = (params) => {
        let color = "info";

        if(params.row["cyclomaticComplexity"] === "LOW") {
            color = "success"
        } else if(params.row["cyclomaticComplexity"] === "MODERATE") {
            color = "warning"
        }else if(params.row["cyclomaticComplexity"] === "HIGH") {
            color = "error"
        }

        return (
            <Chip variant="outlined" size="large" color={color} label={params.row["cyclomaticComplexity"]} />
        )
    }

    const [rows, setRowsData] = React.useState([])
    const positivesTablesColumns = [
        {
            field: 'id',
            headerName: 'Positive Id',
            flex: 1.5,
            minWidth: 200,
            headerAlign: 'left',
            align: 'left'
        },
        {
            field: 'fileName',
            headerName: 'File Name',
            flex: 1,
            minWidth: 200,
            headerAlign: 'left',
            align: 'left',
        },
        {
            field: 'aiCompatibilityScore',
            headerName: 'AI Compatibility Score',
            headerAlign: 'center',
            align: 'center',
            flex: 1,
            minWidth: 80,
            cellClassName: (params) =>
                clsx('super-app', {
                    green: params.value >= 75,
                    yellow: params.value >= 50 && params.value < 75,
                    red: params.value < 50
                }),

        },
        {
            field: 'cyclomaticComplexity',
            headerName: 'Cyclomatic Complexity',
            renderCell: customIcon,
            headerAlign: 'center',
            align: 'center',
            flex: 1,
            minWidth: 100,
        },
        {
            field: 'couplingLevel',
            headerName: 'Coupling Level',
            headerAlign: 'center',
            align: 'center',
            flex: 1,
            minWidth: 120,
        },
        {
            field: 'dynamicCodeConstructs',
            headerName: 'Dynamic Code Constructs',
            headerAlign: 'center',
            align: 'center',
            flex: 1,
            minWidth: 100,
        },
        {
            field: 'relevantComments',
            headerName: 'Relevant Comments',
            headerAlign: 'center',
            align: 'center',
            flex: 1,
            minWidth: 100,
        },
        {
            field: 'viewDetails',
            headerName: 'Details',
            headerAlign: 'center',
            renderCell: renderDetailsButton,
            align: 'center',
            flex: 1,
            minWidth: 100,
        },
    ];

    React.useEffect(() => {
        if (report && report.length > 0) {
            let issues = []
            report.map((obj, index) => {
                obj["response"]["positives"].forEach(issue => {
                    let curr = {}
                    curr["id"] = issue["positiveId"];
                    const words = obj["fileName"].split("/");
                    curr["fileName"] = words[words.length - 1];
                    curr["aiCompatibilityScore"] = obj["response"]["aiCompatibilityScore"];
                    curr["cyclomaticComplexity"] = obj["response"]["cyclomaticComplexity"].toUpperCase();
                    curr["couplingLevel"] = obj["response"]["couplingLevel"].toUpperCase();
                    curr["dynamicCodeConstructs"] = obj["response"]["dynamicCodeConstructs"].toUpperCase();
                    curr["relevantComments"] = obj["response"]["relevantComments"].toUpperCase();
                    curr["issueId"] = issue["issueId"];
                    curr["start"] = issue["start"];
                    curr["end"] = issue["end"];
                    curr["severity"] = issue["severity"];
                    curr["confidence"] = issue["confidence"];
                    curr["description"] = issue["description"];
                    curr["suggestedFix"] = issue["suggestedFix"];
                    issues.push(curr)
                })
            })
            setRowsData(issues)
        }
    }, [report])

    return (
        <DataGrid
            showToolbar
            columnVisibilityModel={{
                id: false
            }}
            checkboxSelection
            rows={rows}
            columns={positivesTablesColumns}
            getRowClassName={(params) =>
                params.indexRelativeToCurrentPage % 2 === 0 ? 'even' : 'odd'
            }
            initialState={{
                pagination: {paginationModel: {pageSize: 20}},
            }}
            sx={{
                '& .super-app-theme--cell': {
                    backgroundColor: 'rgba(224, 183, 60, 0.55)',
                },
                '& .super-app.green': {
                    backgroundColor: 'rgb(198,239,206)',
                    color: 'rgb(0,97,0)'
                },
                '& .super-app.red': {
                    backgroundColor: 'rgb(255,199,206)',
                    color: 'rgb(156,0,6)'
                },
                '& .super-app.yellow': {
                    backgroundColor: 'rgb(255,235,156)',
                    color: 'rgb(156,101,0)'
                },
            }}
            pageSizeOptions={[10, 20, 50]}
            disableColumnResize
            disableMultipleRowSelection={true}
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
                            sx: {mt: 'auto'},
                        },
                        operatorInputProps: {
                            variant: 'outlined',
                            size: 'small',
                            sx: {mt: 'auto'},
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