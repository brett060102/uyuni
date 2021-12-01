import * as React from "react";
import { SsmCounter } from "./ssm-counter";
import SpaRenderer from "core/spa/spa-renderer";

type RendererProps = {
  count?: number;
};

export const renderer = (id: string, { count }: RendererProps = {}) =>
  SpaRenderer.renderNavigationReact(<SsmCounter count={count} />, document.getElementById(id));
