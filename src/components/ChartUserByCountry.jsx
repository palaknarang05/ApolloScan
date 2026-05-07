import * as React from "react";
import PropTypes from "prop-types";
import { PieChart } from "@mui/x-charts/PieChart";
import { useDrawingArea } from "@mui/x-charts/hooks";
import { styled } from "@mui/material/styles";
import Typography from "@mui/material/Typography";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import LinearProgress, {
  linearProgressClasses,
} from "@mui/material/LinearProgress";

const data = [
  { label: "Java", value: 60 },
  { label: "Shell", value: 25 },
  { label: "XML", value: 10 },
  { label: "Other", value: 5 },
];

const StyledText = styled("text", {
  shouldForwardProp: (prop) => prop !== "variant",
})(({ theme }) => ({
  textAnchor: "middle",
  dominantBaseline: "central",
  fill: (theme.vars || theme).palette.text.secondary,
  variants: [
    {
      props: {
        variant: "primary",
      },
      style: {
        fontSize: theme.typography.h5.fontSize,
      },
    },
    {
      props: ({ variant }) => variant !== "primary",
      style: {
        fontSize: theme.typography.body2.fontSize,
      },
    },
    {
      props: {
        variant: "primary",
      },
      style: {
        fontWeight: theme.typography.h5.fontWeight,
      },
    },
    {
      props: ({ variant }) => variant !== "primary",
      style: {
        fontWeight: theme.typography.body2.fontWeight,
      },
    },
  ],
}));

function PieCenterLabel({ primaryText, secondaryText }) {
  const { width, height, left, top } = useDrawingArea();
  const primaryY = top + height / 2 - 10;
  const secondaryY = primaryY + 24;

  return (
    <React.Fragment>
      <StyledText variant="primary" x={left + width / 2} y={primaryY}>
        {primaryText}
      </StyledText>
      <StyledText variant="secondary" x={left + width / 2} y={secondaryY}>
        {secondaryText}
      </StyledText>
    </React.Fragment>
  );
}

PieCenterLabel.propTypes = {
  primaryText: PropTypes.string.isRequired,
  secondaryText: PropTypes.string.isRequired,
};

const colors = [
  "hsl(270, 50%, 90%)",
  "hsl(270, 50%, 80%)",
  "hsl(270, 50%, 70%)",
  "hsl(270, 50%, 60%)",
  "hsl(270, 50%, 50%)",
  "hsl(270, 50%, 40%)",
  "hsl(270, 50%, 30%)",
  "hsl(270, 50%, 20%)",
  "hsl(270, 50%, 15%)",
  "hsl(270, 11.10%, 1.80%)",
];

export default function ChartUserByCountry({ languages, linesOfCode }) {
  let totalLinesOfCode = 0;
  linesOfCode.forEach((item) => (totalLinesOfCode += item.value));

  return (
    languages &&
    linesOfCode && (
      <Card
        variant="outlined"
        sx={{
          display: "flex",
          height: 525,
          overflowY: "scroll",
          flexDirection: "column",
          gap: "8px",
          flexGrow: 1,
        }}
      >
        <CardContent>
          <Typography component="h2" variant="h6">
            Languages
          </Typography>
          <Box sx={{ display: "flex", alignItems: "center" }}>
            <PieChart
              colors={colors}
              margin={{
                left: 80,
                right: 80,
                top: 80,
                bottom: 80,
              }}
              series={[
                {
                  data: linesOfCode,
                  innerRadius: 75,
                  outerRadius: 100,
                  paddingAngle: 0,
                  highlightScope: { fade: "global", highlight: "item" },
                },
              ]}
              height={260}
              width={260}
              hideLegend
            >
              <PieCenterLabel
                primaryText={totalLinesOfCode}
                secondaryText="Total"
              />
            </PieChart>
          </Box>
          {languages.map((country, index) => (
            <Stack
              key={index}
              direction="row"
              sx={{ alignItems: "center", gap: 2, pb: 2 }}
            >
              <Stack sx={{ gap: 1, flexGrow: 1 }}>
                <Stack
                  direction="row"
                  sx={{
                    justifyContent: "space-between",
                    alignItems: "center",
                    gap: 2,
                  }}
                >
                  <Typography variant="body2" sx={{ fontWeight: "500" }}>
                    {country.name}
                  </Typography>
                  <Typography variant="body2" sx={{ color: "text.secondary" }}>
                    {country.value}%
                  </Typography>
                </Stack>
                <LinearProgress
                  variant="determinate"
                  aria-label="Number of users by country"
                  value={country.value}
                  sx={{
                    [`& .${linearProgressClasses.bar}`]: {
                      backgroundColor: colors[index],
                    },
                  }}
                />
              </Stack>
            </Stack>
          ))}
        </CardContent>
      </Card>
    )
  );
}