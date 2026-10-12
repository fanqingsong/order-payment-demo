FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/node:26-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/order-payment-frontend/browser /usr/share/nginx/html
EXPOSE 80
