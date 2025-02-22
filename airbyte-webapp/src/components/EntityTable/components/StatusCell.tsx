import React from "react";

import { SchemaChange, WebBackendConnectionListItem } from "core/api/types/AirbyteClient";
import { FeatureItem, useFeature } from "core/services/features";

import { ChangesStatusIcon } from "./ChangesStatusIcon";
import styles from "./StatusCell.module.scss";
import { StatusCellControl } from "./StatusCellControl";

interface StatusCellProps {
  hasBreakingChange?: boolean;
  enabled?: boolean;
  isSyncing?: boolean;
  isManual?: boolean;
  id: string;
  schemaChange?: SchemaChange;
  connection: WebBackendConnectionListItem;
}

export const StatusCell: React.FC<StatusCellProps> = ({
  enabled,
  isManual,
  id,
  isSyncing,
  schemaChange,
  hasBreakingChange,
  connection,
}) => {
  const allowAutoDetectSchema = useFeature(FeatureItem.AllowAutoDetectSchema);

  return (
    <div className={styles.container} data-testid={`statusCell-${id}`}>
      <StatusCellControl
        enabled={enabled}
        id={id}
        isSyncing={isSyncing}
        isManual={isManual}
        hasBreakingChange={hasBreakingChange}
        connection={connection}
      />
      {allowAutoDetectSchema && hasBreakingChange && <ChangesStatusIcon schemaChange={schemaChange} />}
    </div>
  );
};
