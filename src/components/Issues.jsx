import * as React from 'react';
import Editor from '@monaco-editor/react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import AppTheme from "../shared-theme/AppTheme";
import Grid from '@mui/material/Grid';
import CssBaseline from "@mui/material/CssBaseline";
import Modal from "@mui/material/Modal";

import {
    chartsCustomizations,
    dataGridCustomizations,
    datePickersCustomizations,
    treeViewCustomizations,
} from "../theme/customizations";
import IssuesDataGrid from "./IssuesDataGrid";
import Button from "@mui/material/Button";
import Divider from "@mui/material/Divider";
import axios from "axios";
import {BACKEND_URL, REPO_URL, TOKEN} from "../constants.js";

const xThemeComponents = {
    ...chartsCustomizations,
    ...dataGridCustomizations,
    ...datePickersCustomizations,
    ...treeViewCustomizations,
};

const style = {
    position: "absolute",
    top: "50%",
    left: "50%",
    transform: "translate(-50%, -50%)",
    width: 1200,
    height: 600,
    bgcolor: "background.paper",
    overflowY: "scroll",
    p: 4,
};


export default function Issues() {
    const createIssueHandler = (event) => {
        axios
            .put(`${BACKEND_URL}/create-issues`, {
                "repositoryPath": `${REPO_URL}`,
                "id": [modelIssueId],
            })
            .then((response) => {
                console.log(response)
            });
        console.log(`Intent to create issue ${modelIssueId}`)
        handleClose();
    }

    const [open, setOpen] = React.useState(false);
    const [modelIssueId, setModelIssueId] = React.useState({})
    const [report, setReport] = React.useState();
    const [issueData, setIssueData] = React.useState({})

    React.useEffect(() => {
        axios
            .put(`${BACKEND_URL}/get-reports`, {
                "repositoryPath": `${REPO_URL}`,
            })
            .then((response) => {
                setReport(response.data)
                console.log(report)
            });
    }, [])


    const handleOpen = (issueId) => {
        axios
            .put(`${BACKEND_URL}/get-codes`, {
                "token": TOKEN,
                "repositoryPath": REPO_URL,
                "modulePath": issueId
            })
            .then((response) => {
                setIssueData(response.data)
            });
        setOpen(true);
        setModelIssueId(issueId);
    }
    const handleClose = () => {
        setOpen(false);
        setModelIssueId({});
    }


    return (
        <AppTheme themeComponents={xThemeComponents}>
            <CssBaseline enableColorScheme/>
            <Box sx={{
                width: "100%", maxWidth: {sm: "100%", md: "100%"}, alignItems: "center",
                mx: 3,
                pb: 5,
                mt: {xs: 8, md: 0},
            }}>

                <Typography component="h2" variant="h4" sx={{mb: 2}}>
                    Issues
                </Typography>

                <Grid
                    container
                    spacing={2}
                    columns={12}
                    sx={{mb: (theme) => theme.spacing(2)}}
                >
                    <Grid size={{xs: 12, lg: 12, md: 12}}>
                        <IssuesDataGrid handleOpen={handleOpen} report={report}/>
                    </Grid>

                </Grid>
                <Modal
                    open={open}
                    onClose={handleClose}
                    aria-labelledby="modal-modal-title"
                    aria-describedby="modal-modal-description"
                    disableEnforceFocus
                >
                    <Box sx={style}>
                        <Typography component="h2" variant="h4">
                            Description
                        </Typography>

                        <Divider/>

                        <Typography component="h2" variant="body1" sx={{my: 2}}>
                            {issueData && issueData["description"] ? issueData["description"] : "No description available"}
                        </Typography>

                        <Editor
                            height="50%"
                            defaultLanguage="java"
                            theme="vs-dark"
                            defaultValue={issueData["code"]}
                            options={
                                {
                                    "readOnly": true,
                                    "scrollBeyondLastLine": false,
                                    "minimap": {
                                        "enabled": false
                                    }
                                }
                            }
                        />
                        <Box sx={{fontStyle: 'oblique', m: 1}}>
                            {issueData && issueData["fileName"] ? issueData["fileName"] : "No fileName given"}
                        </Box>

                        <Typography component="h2" variant="h4" sx={{mt: 2}}>
                            Suggested Fix
                        </Typography>

                        <Divider/>

                        <Typography component="h2" variant="body1" sx={{my: 2}}>
                            {issueData && issueData["suggestedFix"] ? issueData["suggestedFix"] : "No fix available"}
                        </Typography>
                        <Button
                            variant="outlined"
                            color="secondary"
                            onClick={createIssueHandler}
                            sx={{my: 2}}
                        >
                            Create Issue
                        </Button>
                    </Box>
                </Modal>
            </Box>
        </AppTheme>

    );
}