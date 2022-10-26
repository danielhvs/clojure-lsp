function dump(o)
   if type(o) == 'table' then
      local s = '{ '
      for k,v in pairs(o) do
         if type(k) ~= 'number' then k = '"'..k..'"' end
         s = s .. '['..k..'] = ' .. dump(v) .. ','
      end
      return s .. '} '
   else
      return tostring(o)
   end
end

clients = vim.lsp.get_active_clients()
for k, client_data in ipairs(clients) do
  id = client_data.id
end

client = vim.lsp.get_client_by_id(id)
result = client.request_sync("clojure/serverInfo/raw", {}, 5000, 15)  
print(dump(result))
print('port = ' .. result.result.port)
print('log-path = ' .. result.result['log-path'])
