import * as React from 'react';
import Avatar from '@mui/material/Avatar';
import Chip from '@mui/material/Chip';

import { SparkLineChart } from '@mui/x-charts/SparkLineChart';

function getDaysInMonth(month, year) {
  const date = new Date(year, month, 0);
  const monthName = date.toLocaleDateString('en-US', {
    month: 'short',
  });
  const daysInMonth = date.getDate();
  const days = [];
  let i = 1;
  while (days.length < daysInMonth) {
    days.push(`${monthName} ${i}`);
    i += 1;
  }
  return days;
}

function renderSparklineCell(params) {
  const data = getDaysInMonth(4, 2024);
  const { value, colDef } = params;

  if (!value || value.length === 0) {
    return null;
  }

  return (
    <div style={{ display: 'flex', alignItems: 'center', height: '100%' }}>
      <SparkLineChart
        data={value}
        width={colDef.computedWidth || 100}
        height={32}
        plotType="bar"
        showHighlight
        showTooltip
        color="hsl(210, 98%, 42%)"
        xAxis={{
          scaleType: 'band',
          data,
        }}
      />
    </div>
  );
}

function renderStatus(status) {
  const colors = {
    Online: 'success',
    Offline: 'default',
  };

  return <Chip label={status} color={colors[status]} size="small" />;
}

export function renderAvatar(params) {
  if (params.value == null) {
    return '';
  }

  return (
    <Avatar
      sx={{
        backgroundColor: params.value.color,
        width: '24px',
        height: '24px',
        fontSize: '0.85rem',
      }}
    >
      {params.value.name.toUpperCase().substring(0, 1)}
    </Avatar>
  );
}

export const columns = [
  {
    field: 'id',
    headerName: 'Id',
    flex: 1.5,
    minWidth: 200,
    headerAlign: 'right',
    align: 'right',
  },
  {
    field: 'fileName',
    headerName: 'File Name',
    flex: 1.5,
    minWidth: 200,
    headerAlign: 'right',
    align: 'right',
  },
  {
    field: 'aiCompatibilityScore',
    headerName: 'AI Compatibility Score',
    headerAlign: 'right',
    align: 'right',
    flex: 1,
    minWidth: 80,
  },
  {
    field: 'cyclomaticComplexity',
    headerName: 'Cyclomatic Complexity',
    headerAlign: 'right',
    align: 'right',
    flex: 1,
    minWidth: 100,
  },
  {
    field: 'couplingLevel',
    headerName: 'Coupling Level',
    headerAlign: 'right',
    align: 'right',
    flex: 1,
    minWidth: 120,
  },
  {
    field: 'dynamicCodeConstructs',
    headerName: 'Dynamic Code Constructs',
    headerAlign: 'right',
    align: 'right',
    flex: 1,
    minWidth: 100,
  },
  {
    field: 'relevantComments',
    headerName: 'Relevant Comments',
    headerAlign: 'right',
    align: 'right',
    flex: 1,
    minWidth: 100,
  },
];

export const rows = [
  {
    id: 1,
    fileName: '  /permissions/IPermProvider.java',
    aiCompatibilityScore: "78",
    cyclomaticComplexity: "low",
    couplingLevel: "loose",
    dynamicCodeConstructs: "low",
    relevantComments: "low",
  },
  {
    id: 1,
    fileName: '  /generator/summary/AccountSummaryGenerator.java',
    aiCompatibilityScore: "78",
    cyclomaticComplexity: "low",
    couplingLevel: "loose",
    dynamicCodeConstructs: "low",
    relevantComments: "low",
  },
  {
    id: 1,
    fileName: '  /permissions/IPermProvider.java',
    aiCompatibilityScore: "78",
    cyclomaticComplexity: "low",
    couplingLevel: "loose",
    dynamicCodeConstructs: "low",
    relevantComments: "low",
  },
  {
    id: 1,
    fileName: '  /permissions/IPermProvider.java',
    aiCompatibilityScore: "78",
    cyclomaticComplexity: "low",
    couplingLevel: "loose",
    dynamicCodeConstructs: "low",
    relevantComments: "low",
  },
  {
    id: 1,
    fileName: '  /permissions/IPermProvider.java',
    aiCompatibilityScore: "78",
    cyclomaticComplexity: "low",
    couplingLevel: "loose",
    dynamicCodeConstructs: "low",
    relevantComments: "low",
  },
  {
    id: 1,
    fileName: '  /permissions/IPermProvider.java',
    aiCompatibilityScore: "78",
    cyclomaticComplexity: "low",
    couplingLevel: "loose",
    dynamicCodeConstructs: "low",
    relevantComments: "low",
  },

];