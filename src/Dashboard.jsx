import * as React from "react";
import { alpha } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";
import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Modal from "@mui/material/Modal";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";
import CircularProgress from "@mui/material/CircularProgress";
import axios from "axios";

import Header from "./components/Header";
import MainGrid from "./components/MainGrid";
import SideMenu from "./components/SideMenu";
import AppTheme from "./shared-theme/AppTheme";
import Reports from "./components/Reports";
import Issues from "./components/Issues";
import Positives from "./components/Positives";
import { IndexContext } from "./IndexProvider";
import { BACKEND_URL, REPO_URL, TOKEN } from "./constants";
import {
    chartsCustomizations,
    dataGridCustomizations,
    datePickersCustomizations,
    treeViewCustomizations,
} from "./theme/customizations";

const xThemeComponents = {
    ...chartsCustomizations,
    ...dataGridCustomizations,
    ...datePickersCustomizations,
    ...treeViewCustomizations,
};

const modalStyle = {
    position: "absolute",
    top: "50%",
    left: "50%",
    transform: "translate(-50%, -50%)",
    width: { xs: "90vw", sm: 560 },
    bgcolor: "background.paper",
    borderRadius: 2,
    boxShadow: 24,
    p: 4,
};

export default function Dashboard(props) {
    const { selectedIndex } = React.useContext(IndexContext);

    const [modalOpen, setModalOpen] = React.useState(false);
    const [snackbar, setSnackbar] = React.useState({ open: false, message: "", severity: "info" });
    const [selectData, setSelectData] = React.useState([]);
    const [repoIndex, setRepoIndex] = React.useState(0);
    const [scanData, setScanData] = React.useState(null);
    const [fullResponse, setFullResponse] = React.useState([]);
    const [scanning, setScanning] = React.useState(false);
    const [initialLoading, setInitialLoading] = React.useState(true);

    const urlRef = React.useRef(null);
    const tokenRef = React.useRef(null);

    const showSnack = (message, severity = "info") =>
        setSnackbar({ open: true, message, severity });

    const loadHistory = React.useCallback(() => {
        return axios.get(`${BACKEND_URL}/all-scanned-repo`).then((res) => {
            if (res.data?.length > 0) {
                setFullResponse(res.data);
                setSelectData(res.data.map((r) => r.pomMetadata).filter(Boolean));
                setScanData(res.data[0]);
                setRepoIndex(0);
            }
        });
    }, []);

    React.useEffect(() => {
        loadHistory()
            .catch(() => showSnack("Backend unreachable. Start the Spring Boot server on port 5555.", "warning"))
            .finally(() => setInitialLoading(false));
    }, [loadHistory]);

    const handleRepoChange = (index) => {
        setScanData(fullResponse[index]);
        setRepoIndex(index);
    };

    const handleScan = () => {
        const url = urlRef.current?.value?.trim();
        const token = tokenRef.current?.value?.trim();
        if (!url) { showSnack("Repository URL is required.", "error"); return; }

        setScanning(true);
        axios
            .put(`${BACKEND_URL}/scan-repo`, { repositoryPath: url, token })
            .then(() => {
                setModalOpen(false);
                return loadHistory();
            })
            .then(() => showSnack("Repository scanned successfully!", "success"))
            .catch((err) => {
                const msg = err.response?.data?.message || err.message || "Scan failed.";
                showSnack(`Scan error: ${msg}`, "error");
            })
            .finally(() => setScanning(false));
    };

    return (
        <AppTheme {...props} themeComponents={xThemeComponents}>
            <Header
                openModal={() => setModalOpen(true)}
                selectOptions={selectData}
                repoIndex={repoIndex}
                setRepoIndex={handleRepoChange}
            />
            <CssBaseline enableColorScheme />

            <Box sx={{ display: "flex" }}>
                <SideMenu
                    openModal={() => setModalOpen(true)}
                    selectOptions={selectData}
                    repoIndex={repoIndex}
                    setRepoIndex={handleRepoChange}
                />

                {selectedIndex === 0 ? (
                    <Box
                        component="main"
                        sx={(theme) => ({
                            flexGrow: 1,
                            backgroundColor: theme.vars
                                ? `rgba(${theme.vars.palette.background.defaultChannel} / 1)`
                                : alpha(theme.palette.background.default, 1),
                            overflow: "auto",
                        })}
                    >
                        <Stack spacing={2} sx={{ alignItems: "center", mx: 3, pb: 5, mt: { xs: 8, md: 0 } }}>
                            {initialLoading ? (
                                <Box sx={{ display: "flex", alignItems: "center", mt: 10, gap: 2 }}>
                                    <CircularProgress size={24} />
                                    <Typography variant="body2" color="text.secondary">Loading previous scans…</Typography>
                                </Box>
                            ) : (
                                <MainGrid scanData={scanData} onNewScan={() => setModalOpen(true)} />
                            )}
                        </Stack>
                    </Box>
                ) : selectedIndex === 1 ? (
                    <Issues />
                ) : selectedIndex === 2 ? (
                    <Positives />
                ) : (
                    <Reports />
                )}
            </Box>

            {/* Scan Modal */}
            <Modal open={modalOpen} onClose={() => !scanning && setModalOpen(false)} disableEnforceFocus>
                <Box sx={modalStyle}>
                    <Stack spacing={3}>
                        <Typography variant="h6" fontWeight={600}>Scan Repository</Typography>
                        <Typography variant="body2" color="text.secondary">
                            Provide a GitHub repository URL and a personal access token with <code>repo:read</code> scope.
                            ApolloScan will analyze your Java source files for AI tool compatibility.
                        </Typography>
                        <TextField
                            label="GitHub Repository URL"
                            variant="filled"
                            fullWidth
                            placeholder="https://github.com/org/repo"
                            inputRef={urlRef}
                            defaultValue={REPO_URL}
                            disabled={scanning}
                        />
                        <TextField
                            label="Access Token"
                            variant="filled"
                            fullWidth
                            type="password"
                            inputRef={tokenRef}
                            defaultValue={TOKEN}
                            helperText="Sent to your local backend only — never stored in the browser."
                            disabled={scanning}
                        />
                        <Stack direction="row" spacing={2} justifyContent="flex-end">
                            <Button variant="text" onClick={() => setModalOpen(false)} disabled={scanning}>Cancel</Button>
                            <Button
                                variant="contained"
                                onClick={handleScan}
                                disabled={scanning}
                                startIcon={scanning ? <CircularProgress size={16} color="inherit" /> : null}
                            >
                                {scanning ? "Scanning…" : "Start Scan"}
                            </Button>
                        </Stack>
                    </Stack>
                </Box>
            </Modal>

            <Snackbar
                open={snackbar.open}
                autoHideDuration={5000}
                onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
                anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
            >
                <Alert
                    severity={snackbar.severity}
                    variant="filled"
                    onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
                >
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </AppTheme>
    );
}
