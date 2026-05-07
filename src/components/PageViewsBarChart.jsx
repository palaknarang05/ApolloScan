import * as React from 'react';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import { BarChart } from '@mui/x-charts/BarChart';
import { useTheme } from '@mui/material/styles';

export default function PageViewsBarChart() {
  const theme = useTheme();
  const colorPalette = [
    (theme.vars || theme).palette.primary.dark,
    (theme.vars || theme).palette.primary.main,
    (theme.vars || theme).palette.primary.light,
  ];

  return (
    <Card variant="outlined" sx={{ width: '100%' }}>
      <CardContent>
        <Typography component="h2" variant="subtitle2" gutterBottom>
          Coverage
        </Typography>
        <Stack sx={{ justifyContent: 'space-between' }}>
          <Stack
            direction="row"
            sx={{
              alignContent: { xs: 'center', sm: 'flex-start' },
              alignItems: 'center',
              gap: 1,
            }}
          >
            <Typography variant="h4" component="p">
              12600
            </Typography>
            <Chip size="small" color="error" label="-2%" />
          </Stack>
          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
            Code coverage for last 5 months
          </Typography>
        </Stack>
        <BarChart
          borderRadius={8}
          colors={colorPalette}
          xAxis={[
            {
              scaleType: 'band',
              categoryGapRatio: 0.5,
              data: ['Feb', 'Mar', 'Apr', 'May', 'Jun'],
              height: 24,
            },
          ]}
          yAxis={[{ width: 50 }]}
          series={[
            {
              id: 'page-views',
              label: 'Lines to Cover',
              data: [2000, 3872, 3998, 4125, 5632, 5969, 6000],
              stack: 'A',
            },
            {
              id: 'downloads',
              label: 'Code Coverage',
              data: [1965, 2600, 3996, 4100, 5600, 5960, 5639],
              stack: 'A',
            },
            {
              id: 'conversions',
              label: 'Duplications',
              data: [1200, 655, 355, 250, 200, 150, 54],
              stack: 'A',
            },
          ]}
          height={250}
          margin={{ left: 0, right: 0, top: 20, bottom: 0 }}
          grid={{ horizontal: true }}
          hideLegend
        />
      </CardContent>
    </Card>
  );
}