import React from 'react'
import axios from "axios";
import {BACKEND_URL, REPO_URL, TOKEN} from "../constants.js";
import AppTheme from "../shared-theme/AppTheme.jsx";
import CssBaseline from "@mui/material/CssBaseline";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import Grid from "@mui/material/Grid";
import Modal from "@mui/material/Modal";
import Divider from "@mui/material/Divider";
import Editor from "@monaco-editor/react";
import PositivesDataGrid from "./PositivesDataGrid.jsx";
import {
    chartsCustomizations,
    dataGridCustomizations,
    datePickersCustomizations,
    treeViewCustomizations
} from "../theme/customizations/index.jsx";

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


const Positives = () => {
    const [report, setReport] = React.useState();
    const [open, setOpen] = React.useState(false);
    const [positiveData, setPositiveData] = React.useState({})

    const handleOpen = (positiveId) => {
        axios
            .put(`${BACKEND_URL}/get-positives`, {
                "token": TOKEN,
                "repositoryPath": REPO_URL,
                "modulePath": positiveId
            })
            .then((response) => {
                setPositiveData(response.data)
            });
        setOpen(true);
    }
    const handleClose = () => {
        setOpen(false);
    }

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
                    Positives
                </Typography>

                <Grid
                    container
                    spacing={2}
                    columns={12}
                    sx={{mb: (theme) => theme.spacing(2)}}
                >
                    <Grid size={{xs: 12, lg: 12, md: 12}}>
                        <PositivesDataGrid handleOpen={handleOpen} report={report}/>
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
                            {positiveData && positiveData["description"] ? positiveData["description"] : "No description available"}
                        </Typography>

                        <Editor
                            height="50%"
                            defaultLanguage="java"
                            theme="vs-dark"
                            defaultValue={positiveData["code"]}
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
                            {positiveData && positiveData["fileName"] ? positiveData["fileName"] : "No fileName given"}
                        </Box>
                    </Box>
                </Modal>
            </Box>
        </AppTheme>
    );
}

export default Positives