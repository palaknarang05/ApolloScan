import * as React from "react";
import Grid from "@mui/material/Grid";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import CircularProgress from "@mui/material/CircularProgress";
import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import AppTheme from "../shared-theme/AppTheme";
import CssBaseline from "@mui/material/CssBaseline";
import SessionsChart from "./SessionsChart";
import PageViewsBarChart from "./PageViewsBarChart";
import CustomizedDataGrid from "./CustomizedDataGrid";
import {
    chartsCustomizations,
    dataGridCustomizations,
    datePickersCustomizations,
    treeViewCustomizations,
} from "../theme/customizations";
import axios from "axios";
import { BACKEND_URL, REPO_URL } from "../constants.js";

const xThemeComponents = {
    ...chartsCustomizations,
    ...dataGridCustomizations,
    ...datePickersCustomizations,
    ...treeViewCustomizations,
};

export default function Reports() {
    const [report, setReport] = React.useState(null);
    const [loading, setLoading] = React.useState(true);
    const [error, setError] = React.useState(null);

    const fetchReports = React.useCallback(() => {
        setLoading(true);
        setError(null);
        axios
            .put(`${BACKEND_URL}/get-reports`, { repositoryPath: REPO_URL })
            .then((res) => setReport(res.data))
            .catch(() => setError("Could not load reports. Make sure the backend is running and a repository has been scanned."))
            .finally(() => setLoading(false));
    }, []);

    React.useEffect(() => { fetchReports(); }, [fetchReports]);

    return (
        <AppTheme themeComponents={xThemeComponents}>
            <CssBaseline enableColorScheme />
            <Box sx={{ width: "100%", maxWidth: { sm: "100%", md: "100%" }, mx: 3, pb: 5, mt: { xs: 8, md: 0 } }}>
                <Typography component="h2" variant="h4" sx={{ mb: 2 }}>Reports</Typography>

                {loading && (
                    <Box sx={{ display: "flex", alignItems: "center", gap: 2, my: 4 }}>
                        <CircularProgress size={24} />
                        <Typography variant="body2" color="text.secondary">Loading scan reports…</Typography>
                    </Box>
                )}

                {error && (
                    <Alert severity="warning" sx={{ mb: 3 }}
                        action={<Button size="small" onClick={fetchReports}>Retry</Button>}>
                        {error}
                    </Alert>
                )}

                {!loading && !error && (
                    <>
                        <Grid container spacing={2} columns={12} sx={{ mb: (t) => t.spacing(2) }}>
                            <Grid size={{ xs: 12, md: 6 }}><SessionsChart report={report} /></Grid>
                            <Grid size={{ xs: 12, md: 6 }}><PageViewsBarChart report={report} /></Grid>
                        </Grid>
                        <Grid container spacing={2} columns={12} sx={{ mb: (t) => t.spacing(2) }}>
                            <Grid size={{ xs: 12 }}><CustomizedDataGrid report={report} /></Grid>
                        </Grid>
                    </>
                )}
            </Box>
        </AppTheme>
    );
}
