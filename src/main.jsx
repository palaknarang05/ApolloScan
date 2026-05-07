import * as React from "react";
import { createRoot } from "react-dom/client";
import CssBaseline from "@mui/material/CssBaseline";
import { ThemeProvider } from "@mui/material/styles";
import theme from "./theme";
import Dashboard from "./Dashboard";
import { IndexProvider } from "./IndexProvider";

const rootElement = document.getElementById("root");
const root = createRoot(rootElement);

root.render(
  <IndexProvider>
    <ThemeProvider theme={theme}>
      {/* CssBaseline kickstart an elegant, consistent, and simple baseline to build upon. */}
      <CssBaseline />
      <Dashboard/>
    </ThemeProvider>
  </IndexProvider>
);