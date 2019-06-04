select *
  from USER
 where ID = /*:id?*/1
   and (
       --#for it : IDs ; delim=' or '
       ID = /*:it*/1
       --#end
       )
