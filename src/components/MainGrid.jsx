import * as React from "react";
import Grid from "@mui/material/Grid";
import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import Card from "@mui/material/Card";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import Markdown from "react-markdown";
import ChartUserByCountry from "./ChartUserByCountry";
import StatCard from "./StatCard";
import AddCircleOutlineRoundedIcon from "@mui/icons-material/AddCircleOutlineRounded";

function buildCard(title, key, scores, lowerIsBetter = false) {
    if (!scores?.length) return null;
    const last = scores[scores.length - 1];
    const first = scores[0];
    const pct = ((last - first) / first) * 100;
    const trend = Math.abs(pct) < 20 ? "neutral" : pct > 0 ? (lowerIsBetter ? "down" : "up") : (lowerIsBetter ? "up" : "down");
    return { title, value: String(last), interval: "Last 30 days", trend, data: scores };
}

export default function MainGrid({ scanData, onNewScan }) {
    if (!scanData) {
        return (
            <Box sx={{ width: "100%", maxWidth: { sm: "100%", md: "1700px" }, textAlign: "center", py: 10 }}>
                <Typography component="h2" variant="h5" sx={{ mb: 1 }}>No repository scanned yet</Typography>
                <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
                    Scan a GitHub repository to see AI compatibility insights.
                </Typography>
                {onNewScan && (
                    <Button variant="contained" startIcon={<AddCircleOutlineRoundedIcon />} onClick={onNewScan}>
                        Scan Repository
                    </Button>
                )}
            </Box>
        );
    }

    const cards = [
        buildCard("Overall Compatibility Score", "ocs", scanData.overallCompatibilityScores),
        buildCard("Cyclomatic Complexity", "cc", scanData.cyclomaticComplexityScores, true),
        buildCard("Checkstyle Issues", "cs", scanData.checkStyleIssueCounts, true),
        buildCard("Incompatible Files", "if", scanData.incompatibleFileCounts, true),
    ].filter(Boolean);

    return (
        <Box sx={{ width: "100%", maxWidth: { sm: "100%", md: "1700px" } }}>
            <Typography component="h2" variant="h4" sx={{ mb: 2 }}>Overview</Typography>

            <Grid container spacing={2} columns={12} sx={{ mb: (t) => t.spacing(2) }}>
                {cards.map((card, i) => (
                    <Grid key={i} size={{ xs: 12, sm: 6, lg: 3 }}>
                        <StatCard {...card} />
                    </Grid>
                ))}
            </Grid>

            <Grid container spacing={2} columns={12} sx={{ mb: (t) => t.spacing(2) }}>
                <Grid size={{ xs: 12, lg: 9 }}>
                    <Card variant="outlined" sx={{ height: 525, flexGrow: 1, overflowY: "scroll", p: 2 }}>
                        <Typography component="h2" variant="h6" sx={{ mb: 2 }}>Project Summary</Typography>
                        <Typography component="div" variant="body1">
                            <Markdown>
                                {scanData.executiveSummary?.replace(/^markdown/, "").replaceAll("`", "") || "No summary available."}
                            </Markdown>
                        </Typography>
                    </Card>
                </Grid>
                <Grid size={{ xs: 12, lg: 3 }}>
                    <Stack gap={2} direction={{ xs: "column", sm: "row", lg: "column" }}>
                        <ChartUserByCountry languages={scanData.languages} linesOfCode={scanData.linesOfCode} />
                    </Stack>
                </Grid>
            </Grid>
        </Box>
    );
}
