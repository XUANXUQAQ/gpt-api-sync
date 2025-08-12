const baseUrl = "http://localhost:7000";

const handleResponse = async (response: Response) => {
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Network response was not ok: ${errorText}`);
  }
  return response.json();
};

export const getServiceInfo = () => fetch(`${baseUrl}/`).then(handleResponse);
export const getServiceStatus = () =>
  fetch(`${baseUrl}/status`).then(handleResponse);
export const getGptLoadInfo = () =>
  fetch(`${baseUrl}/api/gpt-load`).then(handleResponse);
export const getNewApiInfo = () =>
  fetch(`${baseUrl}/api/new-api`).then(handleResponse);
export const getConfig = () => fetch(`${baseUrl}/config`).then(handleResponse);
export const reloadConfig = () =>
  fetch(`${baseUrl}/config/reload`, { method: "POST" }).then(handleResponse);
export const updateConfig = (config: any) =>
  fetch(`${baseUrl}/config`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(config),
  }).then(handleResponse);

export const syncChannels = () =>
  fetch(`${baseUrl}/sync`, { method: "POST" }).then(handleResponse);
