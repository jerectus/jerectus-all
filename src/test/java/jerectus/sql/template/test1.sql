select /*@user*/ USER.*
  from USER
 where ID = /*:id?*/1
   and (
       --#for it : IDs ; delim=' or '
       ID = /*:it*/1
       --#end-for
       )
   and NAME like /*:%name%?*/'%a%'
 order by
       ID --##if orderBy == 1
     , NAME --##if orderBy == 2
