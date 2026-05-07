import * as React from "react";
import MuiAvatar from "@mui/material/Avatar";
import MuiListItemAvatar from "@mui/material/ListItemAvatar";
import MenuItem from "@mui/material/MenuItem";
import ListItemText from "@mui/material/ListItemText";
import ListItemIcon from "@mui/material/ListItemIcon";
import ListSubheader from "@mui/material/ListSubheader";
import Select, { selectClasses } from "@mui/material/Select";
import Divider from "@mui/material/Divider";
import { styled } from "@mui/material/styles";
import AddRoundedIcon from "@mui/icons-material/AddRounded";
import DevicesRoundedIcon from "@mui/icons-material/DevicesRounded";
import SmartphoneRoundedIcon from "@mui/icons-material/SmartphoneRounded";
import ConstructionRoundedIcon from "@mui/icons-material/ConstructionRounded";

const Avatar = styled(MuiAvatar)(({ theme }) => ({
  width: 28,
  height: 28,
  backgroundColor: (theme.vars || theme).palette.background.paper,
  color: (theme.vars || theme).palette.text.secondary,
  border: `1px solid ${(theme.vars || theme).palette.divider}`,
}));

const ListItemAvatar = styled(MuiListItemAvatar)({
  minWidth: 0,
  marginRight: 12,
});

export default function SelectContent({ openModal, selectOptions, repoIndex, setRepoIndex }) {
  const handleChange = (event) => {
    if (event.target.value === 40) {
      openModal();
      return;
    }
    setRepoIndex(event.target.value);
  };

  const hasRepos = selectOptions && selectOptions.length > 0;

  return (
    selectOptions && (
      <Select
        label="Select Project"
        labelId="project-select"
        id="project-simple-select"
        value={hasRepos ? repoIndex : ""}
        displayEmpty
        onChange={handleChange}
        inputProps={{ "aria-label": "Select project" }}
        fullWidth
        sx={{
          maxHeight: 56,
          width: 215,
          "&.MuiList-root": {
            p: "8px",
          },
          [`& .${selectClasses.select}`]: {
            display: "flex",
            alignItems: "center",
            gap: "2px",
            pl: 1,
          },
        }}
      >
        <ListSubheader sx={{ pt: 0 }}>Repositories</ListSubheader>
        {!hasRepos && <MenuItem value="" disabled>No repos scanned yet</MenuItem>}
        {selectOptions.map((option, index) => {
          return (
            <MenuItem value={index}>
              <ListItemAvatar>
                <Avatar alt="Sitemark web">
                  <DevicesRoundedIcon sx={{ fontSize: "1rem" }} />
                </Avatar>
              </ListItemAvatar>
              <ListItemText
                primary={option["artifactId"]}
                secondary={option["version"]}
              />
            </MenuItem>
          );
        })}

        <Divider sx={{ mx: -1 }} />

        <MenuItem value={40}>
          <ListItemIcon>
            <AddRoundedIcon />
          </ListItemIcon>
          <ListItemText primary="Add project" secondary="Git URL" />
        </MenuItem>
      </Select>
    )
  );
}